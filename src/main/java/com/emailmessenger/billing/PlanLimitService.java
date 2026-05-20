package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Enforces per-plan caps so a Free user gets steered into Checkout when
 * they try to push past their tier. The check method throws
 * {@link PlanLimitExceededException}; controllers/services upstream catch
 * it and render the upgrade modal.
 */
@Service
public class PlanLimitService {

    // Statuses that grant the user the entitlements of their paid plan.
    // "incomplete" and "canceled" fall back to FREE so a half-finished
    // checkout doesn't unlock paid limits, and a lapsed user can't keep
    // creating threads after their subscription ends.
    private static final Set<String> ENTITLING_STATUSES = Set.of("trialing", "active", "past_due");

    private final SubscriptionRepository subscriptions;
    private final EmailThreadRepository threads;

    PlanLimitService(SubscriptionRepository subscriptions, EmailThreadRepository threads) {
        this.subscriptions = subscriptions;
        this.threads = threads;
    }

    /**
     * The user's effective plan for entitlement purposes. Returns FREE when
     * there's no subscription, when the row is still {@code incomplete} (mid-
     * checkout), or when it's {@code canceled} / {@code unpaid}.
     */
    @Transactional(readOnly = true)
    public Plan currentPlan(User user) {
        Subscription sub = subscriptions.findByUser(user).orElse(null);
        if (sub == null || sub.getPlan() == null || !ENTITLING_STATUSES.contains(sub.getStatus())) {
            return Plan.FREE;
        }
        return sub.getPlan();
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
}
