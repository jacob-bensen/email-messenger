package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves entitlements. The user's effective plan is read off their Stripe
 * {@link Subscription}: an active or trialing Pro/Business subscription
 * entitles them to that tier; everyone else is on Free and subject to its
 * mailbox + history caps via the {@code enforceCan*} guards.
 */
@Service
public class PlanLimitService {

    private final SubscriptionRepository subscriptions;
    private final EmailThreadRepository threads;
    private final MailAccountRepository mailAccounts;
    private final SavedSearchRepository savedSearches;

    PlanLimitService(SubscriptionRepository subscriptions,
                     EmailThreadRepository threads,
                     MailAccountRepository mailAccounts,
                     SavedSearchRepository savedSearches) {
        this.subscriptions = subscriptions;
        this.threads = threads;
        this.mailAccounts = mailAccounts;
        this.savedSearches = savedSearches;
    }

    /**
     * The user's effective plan for entitlement purposes. A Pro/Business
     * subscription that is currently {@code active} or {@code trialing}
     * entitles the user to that tier; an {@code incomplete} or {@code canceled}
     * subscription (or none at all) falls back to {@link Plan#FREE}.
     */
    @Transactional(readOnly = true)
    public Plan currentPlan(User user) {
        return subscriptions.findByUser(user)
                .filter(PlanLimitService::isEntitled)
                .map(Subscription::getPlan)
                .filter(plan -> plan != null && plan != Plan.FREE)
                .orElse(Plan.FREE);
    }

    private static boolean isEntitled(Subscription sub) {
        String status = sub.getStatus();
        return "active".equalsIgnoreCase(status) || "trialing".equalsIgnoreCase(status);
    }

    @Transactional(readOnly = true)
    public PlanLimits limitsFor(User user) {
        return PlanLimits.forPlan(currentPlan(user));
    }

    /**
     * Throws {@link PlanLimitExceededException} when the user is already at
     * (or somehow past) their plan's thread cap. Call this just before
     * persisting a brand-new thread.
     */
    @Transactional(readOnly = true)
    public void enforceCanCreateThread(User user) {
        Plan plan = currentPlan(user);
        long limit = PlanLimits.forPlan(plan).threads();
        if (limit == PlanLimits.UNLIMITED) {
            return;
        }
        long current = threads.countByOwner(user);
        if (current >= limit) {
            throw new PlanLimitExceededException(plan, PlanLimitKind.THREAD_COUNT, limit, current);
        }
    }

    /**
     * Throws {@link PlanLimitExceededException} when the user is already at
     * their plan's mailbox cap. Call this just before persisting a new
     * {@code MailAccount}. Free caps at 3 mailboxes, so a free user wiring up
     * a 4th gets the upgrade modal; Pro lifts the cap to 5.
     */
    @Transactional(readOnly = true)
    public void enforceCanCreateMailbox(User user) {
        Plan plan = currentPlan(user);
        long limit = PlanLimits.forPlan(plan).mailboxes();
        if (limit == PlanLimits.UNLIMITED) {
            return;
        }
        long current = mailAccounts.countByUser(user);
        if (current >= limit) {
            throw new PlanLimitExceededException(plan, PlanLimitKind.MAILBOX_COUNT, limit, current);
        }
    }

    /**
     * Throws {@link PlanLimitExceededException} when the user is already at
     * their plan's saved-search cap. Saved searches are unlimited on every
     * plan today, so this is a no-op kept for symmetry with the other guards.
     */
    @Transactional(readOnly = true)
    public void enforceCanCreateSavedSearch(User user) {
        Plan plan = currentPlan(user);
        long limit = PlanLimits.forPlan(plan).savedSearches();
        if (limit == PlanLimits.UNLIMITED) {
            return;
        }
        long current = savedSearches.countByOwner(user);
        if (current >= limit) {
            throw new PlanLimitExceededException(plan, PlanLimitKind.SAVED_SEARCH_COUNT, limit, current);
        }
    }
}
