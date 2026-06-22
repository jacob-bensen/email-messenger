package com.emailmessenger.billing;

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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Trial-end conversion email: the final revenue-critical nudge for a
 * trialing subscription whose {@code trial_ends_at} lands inside the
 * next {@link #TRIAL_END_WINDOW}. Past this point Stripe either silently
 * rebills the saved card or the user lapses to canceled — the operator
 * gets one shot at a "pick a plan to keep going" message in the visitor's
 * inbox, alongside the in-app {@link TrialConversionNudgeService} modal.
 *
 * <p>Idempotency is one-shot: the {@code last_trial_end_email_sent_at}
 * stamp on the subscription row gates re-sends, so the daily scheduler
 * can keep ticking over a multi-hour window without double-firing.
 *
 * <p>Reuses the {@link DigestEmailPreference} opt-out token so one
 * unsubscribe still kills every automated marketing channel.
 */
@Service
public class TrialEndConversionService {

    private static final Logger log = LoggerFactory.getLogger(TrialEndConversionService.class);

    static final Duration TRIAL_END_WINDOW = Duration.ofHours(24);

    private final SubscriptionRepository subscriptions;
    private final DigestEmailPreferenceRepository preferences;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    TrialEndConversionService(SubscriptionRepository subscriptions,
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

    public int runTrialEndCycle() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime endingBy = now.plus(TRIAL_END_WINDOW);
        int sent = 0;
        for (Subscription sub : subscriptions.findTrialEndCandidates(endingBy)) {
            try {
                if (sendTrialEndFor(sub, now)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Trial-end conversion send failed for subscription id={}: {}",
                        sub.getId(), e.getMessage());
            }
        }
        return sent;
    }

    @Transactional
    public boolean sendTrialEndFor(Subscription sub, LocalDateTime now) {
        if (sub.getLastTrialEndEmailSentAt() != null) {
            return false;
        }
        User user = sub.getUser();
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return false;
        }
        try {
            mailSender.send(compose(sub, prefs, now));
        } catch (MailException e) {
            log.warn("Trial-end mail send failed for subscription id={}: {}", sub.getId(), e.getMessage());
            return false;
        }
        subscriptions.touchTrialEndEmailSent(sub.getId(), now);
        return true;
    }

    private MimeMessage compose(Subscription sub, DigestEmailPreference prefs, LocalDateTime now) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(sub.getUser().getEmail());
            helper.setSubject(subjectFor(sub, now));
            helper.setText(renderBody(sub, prefs, now), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose trial-end email", e);
        }
        return mime;
    }

    private String subjectFor(Subscription sub, LocalDateTime now) {
        long hours = hoursUntilTrialEnd(sub, now);
        String plan = planLabel(sub.getPlan());
        if (hours <= 0) {
            return "Your ConexusMail " + plan + " trial ends today — keep your setup";
        }
        if (hours <= 24) {
            return "Your ConexusMail " + plan + " trial ends in 24 hours";
        }
        return "Your ConexusMail " + plan + " trial is wrapping up soon";
    }

    private String renderBody(Subscription sub, DigestEmailPreference prefs, LocalDateTime now) {
        User user = sub.getUser();
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String base = site.getBaseUrl();
        String plan = planLabel(sub.getPlan());
        long hours = hoursUntilTrialEnd(sub, now);
        String windowPhrase = hours <= 0 ? "today" : (hours <= 24 ? "in the next 24 hours" : "in a few days");

        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("Your ConexusMail ").append(plan).append(" trial ends ").append(windowPhrase)
          .append(". Pick a plan to keep using your chat-view inbox without\n")
          .append("interruption — your mailbox, threads, and saved searches stay\n")
          .append("exactly as they are.\n\n");
        sb.append("Pick or change plans: ").append(base).append("/pricing\n\n");
        sb.append("Manage payment or cancel from the billing portal: ")
          .append(base).append("/billing\n\n");
        sb.append("Not ready for paid? Downgrade to Free from the same page — 1\n")
          .append("mailbox, 500 threads, no card on file. No trial clock, no auto-\n")
          .append("renewal. You won't lose your threads.\n\n");
        sb.append("Want to revisit what ConexusMail looks like first? Live demo: ")
          .append(base).append("/demo\n\n");
        sb.append("Don't want these emails? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private static long hoursUntilTrialEnd(Subscription sub, LocalDateTime now) {
        if (sub.getTrialEndsAt() == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(now, sub.getTrialEndsAt());
    }

    private static String planLabel(Plan plan) {
        if (plan == null) {
            return "";
        }
        return switch (plan) {
            case PRO -> "Pro";
            case BUSINESS -> "Business";
            case FREE -> "";
        };
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
