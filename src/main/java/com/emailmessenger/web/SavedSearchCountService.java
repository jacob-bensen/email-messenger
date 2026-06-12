package com.emailmessenger.web;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds {@link SavedSearchView}s with live match counts and a
 * "new since last visit" delta for the inbox rail.
 *
 * <p>Match count uses the same repository methods that power the inbox
 * results page so the rail figure stays consistent with what the user
 * actually sees after clicking through. Free users still get
 * subject/participant matches only (body content is paid-only) — same as
 * the result page. The "new" subset re-runs the same query with the
 * since-filter floor raised to {@code lastViewedAt} (falling back to
 * {@code createdAt} on never-opened rows so the badge doesn't bleed every
 * historical thread on a brand-new saved search).
 */
@Service
public class SavedSearchCountService {

    private static final PageRequest ONE = PageRequest.of(0, 1);

    private final EmailThreadRepository threads;
    private final PlanLimitService planLimits;
    private final Clock clock;

    SavedSearchCountService(EmailThreadRepository threads,
                            PlanLimitService planLimits,
                            Clock clock) {
        this.threads = threads;
        this.planLimits = planLimits;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SavedSearchView> viewsFor(User owner, List<SavedSearch> saved) {
        if (saved.isEmpty()) {
            return List.of();
        }
        Plan plan = planLimits.currentPlan(owner);
        return saved.stream().map(s -> withCounts(owner, plan, s)).toList();
    }

    private SavedSearchView withCounts(User owner, Plan plan, SavedSearch s) {
        long match = countMatching(owner, plan, s, null);
        LocalDateTime cutoff = s.getLastViewedAt() != null ? s.getLastViewedAt() : s.getCreatedAt();
        long fresh = (cutoff == null) ? match : countMatching(owner, plan, s, cutoff);
        return SavedSearchView.withCounts(s, match, fresh);
    }

    private long countMatching(User owner, Plan plan, SavedSearch s, LocalDateTime newerThan) {
        LocalDateTime since = sinceForPreset(s.getSincePreset());
        if (newerThan != null) {
            since = (since == null || newerThan.isAfter(since)) ? newerThan : since;
        }
        boolean unread = s.isRequireUnread();
        boolean att = s.isRequireAttachments();
        String q = s.getQuery();
        String sender = s.getSenderEmail();
        boolean hasQuery = q != null && !q.isBlank();
        boolean hasSender = sender != null && !sender.isBlank();
        if (!hasQuery) {
            if (!hasSender) {
                return threads.findByOwnerFiltered(owner, since, unread, att, ONE).getTotalElements();
            }
            return threads.findByOwnerAndSender(owner, sender, since, unread, att, ONE).getTotalElements();
        }
        if (plan != Plan.FREE) {
            return threads.searchIncludingBody(owner, q, sender, since, unread, att, ONE).getTotalElements();
        }
        return threads.search(owner, q, sender, since, unread, att, ONE).getTotalElements();
    }

    private LocalDateTime sinceForPreset(String preset) {
        if (preset == null) {
            return null;
        }
        return switch (preset) {
            case "7d" -> LocalDateTime.now(clock).minusDays(7);
            case "30d" -> LocalDateTime.now(clock).minusDays(30);
            case "90d" -> LocalDateTime.now(clock).minusDays(90);
            default -> null;
        };
    }
}
