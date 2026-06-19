package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves entitlements. ConexusMail unlocks every feature for every account,
 * so {@link #currentPlan} is the top tier and the {@code enforceCan*} caps are
 * effectively no-ops. The class is kept (rather than ripped out) so callers and
 * the upgrade-modal plumbing still compile and can be re-enabled if pricing
 * ever returns.
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
     * The user's effective plan for entitlement purposes. ConexusMail (Bensen
     * LLC) ships every feature to every account, so this is unconditionally the
     * top tier — paid features are unlocked for all accounts regardless of any
     * Stripe subscription state. Billing/checkout and admin analytics read the
     * raw {@link Subscription} directly and are unaffected by this.
     */
    @Transactional(readOnly = true)
    public Plan currentPlan(User user) {
        return Plan.ENTERPRISE;
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
     * {@code MailAccount}. The Free plan caps at 1 mailbox, so a free user
     * who tries to wire up a second mailbox gets the upgrade modal.
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
     * their plan's saved-search cap. Free is capped at 1 so the second save
     * lands the upgrade modal; paid plans are unlimited.
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
