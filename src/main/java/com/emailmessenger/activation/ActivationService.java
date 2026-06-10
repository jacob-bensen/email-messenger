package com.emailmessenger.activation;

import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
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
import java.util.UUID;

/**
 * Day-1 "connect your mailbox" activation email. Sweeps signups that
 * cleared {@link #ACTIVATION_DELAY} but haven't connected any mail
 * account — the biggest activation leak in the funnel, since the
 * existing reengagement sweep is gated on unread-thread count and
 * never fires for a user with zero threads.
 *
 * <p>Idempotency is one-shot: a single {@code last_activation_nudge_sent_at}
 * stamp gates re-sends. Unlike the reengagement sweep (which can re-fire
 * if a user comes back and re-disappears), the activation nudge is
 * intentionally fire-once — if the visitor still hasn't connected after
 * the first nudge, follow-up cadence belongs to later milestones, not
 * a tick-on-tick re-send.
 *
 * <p>Reuses {@link DigestEmailPreference} so the same unsubscribe link
 * kills every automated marketing email; an opted-out user is skipped
 * even if every other gate clears.
 */
@Service
public class ActivationService {

    private static final Logger log = LoggerFactory.getLogger(ActivationService.class);

    static final Duration ACTIVATION_DELAY = Duration.ofHours(24);
    static final Duration ACTIVATION_FOLLOWUP_DELAY = Duration.ofHours(72);
    static final Duration ACTIVATION_LAST_CHANCE_DELAY = Duration.ofHours(168);

    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final DigestEmailPreferenceRepository preferences;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@mailaim.app}")
    private String fromAddress = "noreply@mailaim.app";

    ActivationService(UserRepository users,
                      SubscriptionRepository subscriptions,
                      DigestEmailPreferenceRepository preferences,
                      JavaMailSender mailSender,
                      SiteProperties site,
                      Clock clock) {
        this.users = users;
        this.subscriptions = subscriptions;
        this.preferences = preferences;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    public int runActivationCycle() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(ACTIVATION_DELAY);
        int sent = 0;
        for (User user : users.findActivationCandidates(cutoff)) {
            try {
                if (sendActivationFor(user, now)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Activation nudge send failed for user id={}: {}",
                        user.getId(), e.getMessage());
            }
        }
        return sent;
    }

    public int runActivationFollowupCycle() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(ACTIVATION_FOLLOWUP_DELAY);
        int sent = 0;
        for (User user : users.findActivationFollowupCandidates(cutoff)) {
            try {
                if (sendActivationFollowupFor(user, now)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Activation follow-up send failed for user id={}: {}",
                        user.getId(), e.getMessage());
            }
        }
        return sent;
    }

    public int runActivationLastChanceCycle() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(ACTIVATION_LAST_CHANCE_DELAY);
        int sent = 0;
        for (User user : users.findActivationLastChanceCandidates(cutoff)) {
            try {
                if (sendActivationLastChanceFor(user, now)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Activation last-chance send failed for user id={}: {}",
                        user.getId(), e.getMessage());
            }
        }
        return sent;
    }

    @Transactional
    public boolean sendActivationFor(User user, LocalDateTime now) {
        if (user.getLastActivationNudgeSentAt() != null) {
            return false;
        }
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return false;
        }
        try {
            mailSender.send(compose(user, prefs));
        } catch (MailException e) {
            log.warn("Activation mail send failed for user id={}: {}", user.getId(), e.getMessage());
            return false;
        }
        users.touchActivationNudgeSent(user.getId(), now);
        return true;
    }

    @Transactional
    public boolean sendActivationFollowupFor(User user, LocalDateTime now) {
        if (user.getLastActivationFollowupSentAt() != null) {
            return false;
        }
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return false;
        }
        try {
            mailSender.send(composeFollowup(user, prefs));
        } catch (MailException e) {
            log.warn("Activation follow-up mail send failed for user id={}: {}", user.getId(), e.getMessage());
            return false;
        }
        users.touchActivationFollowupSent(user.getId(), now);
        return true;
    }

    @Transactional
    public boolean sendActivationLastChanceFor(User user, LocalDateTime now) {
        if (user.getLastActivationLastChanceSentAt() != null) {
            return false;
        }
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return false;
        }
        boolean paidIntent = hasPaidIntent(user);
        try {
            mailSender.send(composeLastChance(user, prefs, paidIntent));
        } catch (MailException e) {
            log.warn("Activation last-chance mail send failed for user id={}: {}", user.getId(), e.getMessage());
            return false;
        }
        users.touchActivationLastChanceSent(user.getId(), now);
        return true;
    }

    // Paid intent = the signup picked PERSONAL/TEAM/ENTERPRISE at /pricing
    // and got far enough into Stripe Checkout to materialize a Subscription
    // row (status ranges from "incomplete" through "trialing"/"active").
    // Free-tier signups have no Subscription at all; users who explicitly
    // picked Free or whose paid attempt was wiped get FREE plan. Both
    // collapse to the "no trial clock, take your time" framing.
    private boolean hasPaidIntent(User user) {
        Subscription sub = subscriptions.findByUser(user).orElse(null);
        if (sub == null) {
            return false;
        }
        Plan plan = sub.getPlan();
        return plan != null && plan != Plan.FREE;
    }

    private MimeMessage compose(User user, DigestEmailPreference prefs) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Connect your mailbox to see it as a chat");
            helper.setText(renderBody(user, prefs), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose activation email", e);
        }
        return mime;
    }

    private MimeMessage composeFollowup(User user, DigestEmailPreference prefs) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("See MailIM in action — no mailbox needed");
            helper.setText(renderFollowupBody(user, prefs), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose activation follow-up email", e);
        }
        return mime;
    }

    private MimeMessage composeLastChance(User user, DigestEmailPreference prefs, boolean paidIntent) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject(paidIntent
                    ? "Your MailIM trial is winding down — connect to use it"
                    : "Still curious about MailIM? Free is here whenever you're ready");
            helper.setText(renderLastChanceBody(user, prefs, paidIntent), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose activation last-chance email", e);
        }
        return mime;
    }

    private String renderBody(User user, DigestEmailPreference prefs) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("Welcome to MailIM! You signed up yesterday but haven't connected\n")
          .append("a mailbox yet — without one, there's nothing to render as a chat.\n\n");
        sb.append("Connect in about 60 seconds: ").append(base).append("/mailboxes/new\n\n");
        sb.append("Curious what it looks like first? Here's a live demo conversation: ")
          .append(base).append("/demo\n\n");
        sb.append("Don't want these emails? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private String renderFollowupBody(User user, DigestEmailPreference prefs) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("No pressure on connecting a mailbox — handing over IMAP credentials\n")
          .append("is a real ask. Want to see what you'd get first?\n\n");
        sb.append("Open the live demo (no signup, no credentials): ")
          .append(base).append("/demo\n\n");
        sb.append("It's a real multi-participant thread rendered as a chat — bubbles,\n")
          .append("avatars, day separators, dark mode — exactly how your own inbox\n")
          .append("would look.\n\n");
        sb.append("When you're ready, connecting your mailbox takes about 60 seconds: ")
          .append(base).append("/mailboxes/new\n\n");
        sb.append("Don't want these emails? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private String renderLastChanceBody(User user, DigestEmailPreference prefs, boolean paidIntent) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        if (paidIntent) {
            sb.append("It's been a week since you signed up for a MailIM trial, and the\n")
              .append("14-day clock is still running even though there's no mailbox\n")
              .append("connected yet. Here's what you're missing: your real inbox\n")
              .append("rendered as a chat — bubbles, avatars, day separators, dark mode\n")
              .append("— instead of a wall of nested-quote replies.\n\n");
            sb.append("Connect in about 60 seconds and use the rest of your trial: ")
              .append(base).append("/mailboxes/new\n\n");
            sb.append("Or downgrade to Free anytime from your billing page — 1 mailbox,\n")
              .append("500 threads, no card on file, no trial clock: ")
              .append(base).append("/billing\n\n");
        } else {
            sb.append("It's been a week since you signed up, and you picked Free — so\n")
              .append("there's no trial clock and no card on file. Here's what you're\n")
              .append("missing while we wait: your real inbox rendered as a chat —\n")
              .append("bubbles, avatars, day separators, dark mode — instead of a wall\n")
              .append("of nested-quote replies.\n\n");
            sb.append("Free covers 1 mailbox and 500 threads — enough for most personal\n")
              .append("use. Connect when you're ready, takes about 60 seconds: ")
              .append(base).append("/mailboxes/new\n\n");
        }
        sb.append("Still want to see it working without signing in? Live demo: ")
          .append(base).append("/demo\n\n");
        sb.append("This is the last activation email you'll get from us. Don't want\n")
          .append("any further updates either? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
