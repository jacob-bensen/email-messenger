package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.CancellationReason;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.web.SiteProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Operator-initiated win-back outreach for canceled paid subscribers.
 * Triggered per row from the at-risk retention queue on
 * {@code /admin/revenue}: the "Send win-back" button POSTs the
 * subscription id, the service composes a reason-aware templated email
 * through {@link JavaMailSender}, and stamps
 * {@code last_win_back_email_sent_at} so the row renders as "Sent X ago"
 * on the next refresh — the operator can't double-fire on the same row.
 *
 * <p>Gates a send on, in order: the row still being a canceled paid
 * subscription (a re-subscribed customer is no longer a win-back target),
 * the {@link DigestEmailPreference} opt-out flag (same one-token
 * unsubscribe path the trial-end email respects), and the existing stamp
 * being null. Mail-failure leaves the stamp unset so a retry works.
 */
@Service
public class WinBackOutreachService {

    private static final Logger log = LoggerFactory.getLogger(WinBackOutreachService.class);

    private final SubscriptionRepository subscriptions;
    private final DigestEmailPreferenceRepository preferences;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    WinBackOutreachService(SubscriptionRepository subscriptions,
                           DigestEmailPreferenceRepository preferences,
                           JavaMailSender mailSender,
                           SiteProperties site,
                           Clock clock) {
        this.subscriptions = subscriptions;
        this.preferences = preferences;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    public enum Outcome {
        SENT,
        ALREADY_SENT,
        OPTED_OUT,
        NOT_CANCELED,
        UNSUPPORTED_PLAN,
        NOT_FOUND,
        MAIL_FAILED
    }

    @Transactional
    public Outcome sendWinBackFor(Long subscriptionId) {
        Subscription sub = subscriptions.findById(subscriptionId).orElse(null);
        if (sub == null) {
            return Outcome.NOT_FOUND;
        }
        if (!"canceled".equalsIgnoreCase(sub.getStatus())) {
            return Outcome.NOT_CANCELED;
        }
        if (sub.getPlan() == null || sub.getPlan() == Plan.FREE) {
            return Outcome.UNSUPPORTED_PLAN;
        }
        if (sub.getLastWinBackEmailSentAt() != null) {
            return Outcome.ALREADY_SENT;
        }
        User user = sub.getUser();
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return Outcome.OPTED_OUT;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            mailSender.send(compose(sub, prefs));
        } catch (MailException e) {
            log.warn("Win-back mail send failed for subscription id={}: {}",
                    sub.getId(), e.getMessage());
            return Outcome.MAIL_FAILED;
        }
        subscriptions.touchWinBackEmailSent(sub.getId(), now);
        return Outcome.SENT;
    }

    private MimeMessage compose(Subscription sub, DigestEmailPreference prefs) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(sub.getUser().getEmail());
            helper.setSubject(subjectFor(sub));
            helper.setText(renderBody(sub, prefs), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose win-back email", e);
        }
        return mime;
    }

    private String subjectFor(Subscription sub) {
        String plan = planLabel(sub.getPlan());
        return switch (reasonOf(sub)) {
            case TOO_EXPENSIVE -> "Come back to ConexusMail " + plan + " — let's find a fit";
            case MISSING_FEATURE -> "ConexusMail " + plan + " — what were we missing?";
            case SWITCHING -> "Hand-off help if you ever come back to ConexusMail " + plan;
            case TEMPORARY -> "Your ConexusMail " + plan + " setup is still here when you're ready";
            case OTHER -> "Anything we can do to bring you back to ConexusMail " + plan + "?";
        };
    }

    private String renderBody(Subscription sub, DigestEmailPreference prefs) {
        User user = sub.getUser();
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String base = site.getBaseUrl();
        String plan = planLabel(sub.getPlan());
        String cadence = (sub.getBillingPeriod() == BillingPeriod.ANNUAL) ? "annual" : "monthly";

        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("We noticed you canceled your ConexusMail ").append(plan).append(' ')
          .append(cadence).append(" plan recently. Your threads, saved searches,\n")
          .append("and mailbox connections are still on your account exactly as you\n")
          .append("left them — nothing's been deleted.\n\n");
        sb.append(reasonSpecificBlock(reasonOf(sub), plan));
        sb.append("\nRestart whenever you're ready: ").append(base).append("/pricing\n\n");
        sb.append("Or hit reply to this email — a real person reads it, and we'll\n")
          .append("happily extend a one-month credit on the ").append(plan)
          .append(" plan to give it a second look.\n\n");
        sb.append("Don't want these emails? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private String reasonSpecificBlock(CancellationReason reason, String plan) {
        return switch (reason) {
            case TOO_EXPENSIVE -> ""
                + "You picked \"too expensive\" on the way out. If price was the\n"
                + "blocker, we can put you back on the " + plan + " plan at $5 off the\n"
                + "first three months — just hit reply and we'll set it up.\n";
            case MISSING_FEATURE -> ""
                + "You flagged a missing feature on the way out. We genuinely want to\n"
                + "know which one — reply with a single line and it goes straight\n"
                + "onto the roadmap. If the gap closes you'll be the first to hear.\n";
            case SWITCHING -> ""
                + "If a different tool is working better right now, that's fine — we'd\n"
                + "still love to learn which one and what tipped the call, so we can\n"
                + "close the gap before the next person looks.\n";
            case TEMPORARY -> ""
                + "Since this was a temporary pause, no rush — drop back in whenever\n"
                + "your schedule clears. Your threads, saved searches, and mailbox\n"
                + "setup pick up exactly where you left them.\n";
            case OTHER -> ""
                + "We didn't get a reason on the way out, so this one's a long shot —\n"
                + "if there's anything we can change to bring you back, hit reply and\n"
                + "let us know. Real human on the other end.\n";
        };
    }

    private static CancellationReason reasonOf(Subscription sub) {
        return sub.getCancellationReason() == null ? CancellationReason.OTHER : sub.getCancellationReason();
    }

    private static String planLabel(Plan plan) {
        return switch (plan) {
            case PERSONAL -> "Personal";
            case TEAM -> "Team";
            case ENTERPRISE -> "Enterprise";
            case FREE -> "";
        };
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
