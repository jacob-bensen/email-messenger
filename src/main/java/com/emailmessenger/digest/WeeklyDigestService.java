package com.emailmessenger.digest;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.web.SiteProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Weekly "what's new in your saved searches" digest. Iterates paid users
 * who have at least one saved search, builds a per-saved-search list of
 * threads updated since the previous digest (floored at seven days), and
 * sends one plain-text email per user. Free users are intentionally
 * skipped — the digest is the upgrade hook for engaged users hitting the
 * 1-saved-search Free cap.
 *
 * <p>Lazily provisions a {@link DigestEmailPreference} row with a UUID
 * opt-out token on the first send. The token is surfaced in the footer
 * so the user can unsubscribe with a single unauthenticated GET; the
 * scheduler honours {@code opted_out} on subsequent runs.
 *
 * <p>The same code path is the entry point for the scheduler (which calls
 * {@link #runDigestCycle()}) and for direct testing (which can call
 * {@link #sendDigestFor(User)} for a single user without the full sweep).
 */
@Service
public class WeeklyDigestService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestService.class);

    static final Duration LOOKBACK = Duration.ofDays(7);
    static final int MAX_THREADS_PER_SECTION = 5;
    private static final Pageable SECTION_PAGE = PageRequest.of(0, MAX_THREADS_PER_SECTION);

    private final SavedSearchRepository savedSearches;
    private final EmailThreadRepository threads;
    private final DigestEmailPreferenceRepository preferences;
    private final PlanLimitService planLimits;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;

    @Value("${spring.mail.username:noreply@mailaim.app}")
    private String fromAddress = "noreply@mailaim.app";

    WeeklyDigestService(SavedSearchRepository savedSearches,
                        EmailThreadRepository threads,
                        DigestEmailPreferenceRepository preferences,
                        PlanLimitService planLimits,
                        JavaMailSender mailSender,
                        SiteProperties site,
                        Clock clock) {
        this.savedSearches = savedSearches;
        this.threads = threads;
        this.preferences = preferences;
        this.planLimits = planLimits;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    /**
     * Sweep every user with at least one saved search. Per-user failures
     * are logged and skipped so a single bad row can't stall the cycle.
     */
    public int runDigestCycle() {
        int sent = 0;
        for (User owner : savedSearches.findDistinctOwners()) {
            try {
                if (sendDigestFor(owner)) {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Digest send failed for user id={}: {}", owner.getId(), e.getMessage());
            }
        }
        return sent;
    }

    /**
     * Compose and send the digest for one user. Returns true if mail
     * actually went out, false on any skip (free plan, opted out, no new
     * matches, …). Updates {@code last_sent_at} when sending succeeds.
     */
    @Transactional
    public boolean sendDigestFor(User user) {
        Plan plan = planLimits.currentPlan(user);
        if (plan == Plan.FREE) {
            return false;
        }
        DigestEmailPreference prefs = preferences.findByUser(user)
                .orElseGet(() -> preferences.save(new DigestEmailPreference(user, newToken())));
        if (prefs.isOptedOut()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(LOOKBACK);

        List<SavedSearch> searches = savedSearches.findByOwnerOrderByCreatedAtAsc(user);
        List<DigestSection> sections = new ArrayList<>();
        for (SavedSearch s : searches) {
            List<EmailThread> matches = findNewMatches(user, plan, s, cutoff);
            if (!matches.isEmpty()) {
                sections.add(new DigestSection(s.getName(), matches));
            }
        }
        if (sections.isEmpty()) {
            return false;
        }

        try {
            mailSender.send(compose(user, prefs, sections));
        } catch (MailException e) {
            log.warn("Digest mail send failed for user id={}: {}", user.getId(), e.getMessage());
            return false;
        }
        prefs.setLastSentAt(now);
        preferences.save(prefs);
        return true;
    }

    private MimeMessage compose(User user, DigestEmailPreference prefs, List<DigestSection> sections) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject(buildSubject(sections));
            helper.setText(renderBody(user, prefs, sections), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose weekly digest", e);
        }
        return mime;
    }

    private String buildSubject(List<DigestSection> sections) {
        int total = sections.stream().mapToInt(s -> s.threads().size()).sum();
        return "MailIM weekly digest — " + total + " new " + (total == 1 ? "thread" : "threads");
    }

    private String renderBody(User user, DigestEmailPreference prefs, List<DigestSection> sections) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String base = site.getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("Here's what's new in your saved searches over the last 7 days:\n\n");
        for (DigestSection section : sections) {
            sb.append("* ").append(section.name())
              .append(" (").append(section.threads().size())
              .append(section.threads().size() == 1 ? " new thread)" : " new threads)")
              .append('\n');
            for (EmailThread t : section.threads()) {
                sb.append("    - ").append(safeSubject(t.getSubject()));
                if (t.getId() != null) {
                    sb.append("  ").append(base).append("/threads/").append(t.getId());
                }
                sb.append('\n');
            }
            sb.append('\n');
        }
        sb.append("Open your inbox: ").append(base).append("/threads\n\n");
        sb.append("Don't want these emails? Unsubscribe: ")
          .append(base).append("/digest/opt-out?token=").append(prefs.getOptOutToken()).append('\n');
        return sb.toString();
    }

    private static String safeSubject(String subject) {
        if (subject == null || subject.isBlank()) return "(no subject)";
        return subject.replaceAll("\\s+", " ").trim();
    }

    private List<EmailThread> findNewMatches(User owner, Plan plan, SavedSearch s, LocalDateTime cutoff) {
        LocalDateTime since = sinceForPreset(s.getSincePreset());
        since = (since == null || cutoff.isAfter(since)) ? cutoff : since;
        boolean unread = s.isRequireUnread();
        boolean att = s.isRequireAttachments();
        String q = s.getQuery();
        String sender = s.getSenderEmail();
        boolean hasQuery = q != null && !q.isBlank();
        boolean hasSender = sender != null && !sender.isBlank();
        if (!hasQuery) {
            if (!hasSender) {
                return threads.findByOwnerFiltered(owner, since, unread, att, SECTION_PAGE).getContent();
            }
            return threads.findByOwnerAndSender(owner, sender, since, unread, att, SECTION_PAGE).getContent();
        }
        if (plan != Plan.FREE) {
            return threads.searchIncludingBody(owner, q, sender, since, unread, att, SECTION_PAGE).getContent();
        }
        return threads.search(owner, q, sender, since, unread, att, SECTION_PAGE).getContent();
    }

    private LocalDateTime sinceForPreset(String preset) {
        if (preset == null) return null;
        return switch (preset) {
            case "7d" -> LocalDateTime.now(clock).minusDays(7);
            case "30d" -> LocalDateTime.now(clock).minusDays(30);
            case "90d" -> LocalDateTime.now(clock).minusDays(90);
            default -> null;
        };
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record DigestSection(String name, List<EmailThread> threads) {}
}
