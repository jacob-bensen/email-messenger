package com.emailmessenger.reengagement;

import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.EmailThreadRepository;
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
 * Re-engagement email after seven days of inactivity. Sweeps every
 * {@link User} whose most recent login or {@code /threads} GET is older
 * than {@link #INACTIVITY_THRESHOLD}, skipping users with no unread
 * threads to nudge them about and users who opted out of the digest
 * channel.
 *
 * <p>Idempotency comes from {@code users.last_reengagement_sent_at}: a
 * second nudge only fires after the user has been seen again (login OR
 * inbox visit) and then re-disappeared, so a permanently-dormant account
 * gets exactly one email per "round" of inactivity rather than one per
 * scheduler tick.
 *
 * <p>Reuses the {@link DigestEmailPreference} opt-out token so a single
 * unsubscribe link kills every automated marketing email to that user.
 */
@Service
public class ReengagementService {

    private static final Logger log = LoggerFactory.getLogger(ReengagementService.class);

    static final Duration INACTIVITY_THRESHOLD = Duration.ofDays(7);

    private final UserRepository users;
    private final EmailThreadRepository threads;
    private final DigestEmailPreferenceRepository preferences;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@mailaim.app}")
    private String fromAddress = "noreply@mailaim.app";

    ReengagementService(UserRepository users,
                        EmailThreadRepository threads,
                        DigestEmailPreferenceRepository preferences,
                        JavaMailSender mailSender,
                        SiteProperties site,
                        Clock clock) {
        this.users = users;
        this.threads = threads;
        this.preferences = preferences;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    public int runReengagementCycle() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(INACTIVITY_THRESHOLD);
        int sent = 0;
        for (User user : users.findDormantSince(cutoff)) {
            try {
                if (sendReengagementFor(user, now)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Re-engagement send failed for user id={}: {}", user.getId(), e.getMessage());
            }
        }
        return sent;
    }

    @Transactional
    public boolean sendReengagementFor(User user, LocalDateTime now) {
        LocalDateTime lastActive = effectiveLastActivity(user);
        LocalDateTime lastSent = user.getLastReengagementSentAt();
        if (lastSent != null && !lastSent.isBefore(lastActive)) {
            return false;
        }
        long unread = threads.countByOwnerAndUnreadTrue(user);
        if (unread == 0) {
            return false;
        }
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return false;
        }
        try {
            mailSender.send(compose(user, prefs, unread, lastActive, now));
        } catch (MailException e) {
            log.warn("Re-engagement mail send failed for user id={}: {}", user.getId(), e.getMessage());
            return false;
        }
        users.touchReengagementSent(user.getId(), now);
        return true;
    }

    private MimeMessage compose(User user, DigestEmailPreference prefs, long unread,
                                LocalDateTime lastActive, LocalDateTime now) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject(buildSubject(unread));
            helper.setText(renderBody(user, prefs, unread, lastActive, now), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose re-engagement email", e);
        }
        return mime;
    }

    private String buildSubject(long unread) {
        return unread == 1
                ? "1 unread thread waiting in your MailIM inbox"
                : unread + " unread threads waiting in your MailIM inbox";
    }

    private String renderBody(User user, DigestEmailPreference prefs, long unread,
                              LocalDateTime lastActive, LocalDateTime now) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        long daysAway = Math.max(1L, Duration.between(lastActive, now).toDays());
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("It's been ").append(daysAway).append(daysAway == 1 ? " day" : " days")
          .append(" since you last opened MailIM, and you have ")
          .append(unread).append(unread == 1 ? " unread thread" : " unread threads")
          .append(" waiting.\n\n");
        sb.append("Open your inbox: ").append(base).append("/threads\n\n");
        sb.append("Don't want these emails? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private LocalDateTime effectiveLastActivity(User user) {
        LocalDateTime login = user.getLastLoginAt() != null ? user.getLastLoginAt() : user.getCreatedAt();
        LocalDateTime visit = user.getLastInboxVisitAt() != null
                ? user.getLastInboxVisitAt() : user.getCreatedAt();
        return login.isAfter(visit) ? login : visit;
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
