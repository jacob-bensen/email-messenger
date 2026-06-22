package com.emailmessenger.admin;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.repository.PlanChangeEventRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * "Pro-plan adoption — last 30 days" card for {@code /admin/revenue}.
 *
 * <p>Reports Free→Pro conversions logged by
 * {@link com.emailmessenger.billing.BillingService} into
 * {@code plan_change_events}, plus the count of subscribers currently entitled
 * to Pro/Business. The window-scoped conversion count is also computed for the
 * prior 30-day window so the operator can see the conversion lift against the
 * preceding baseline.
 */
@Service
public class ProAdoptionMetricsService {

    static final int WINDOW_DAYS = 30;

    private final PlanChangeEventRepository planChanges;
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    ProAdoptionMetricsService(PlanChangeEventRepository planChanges,
                              SubscriptionRepository subscriptions,
                              Clock clock) {
        this.planChanges = planChanges;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProAdoptionMetrics snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);
        LocalDateTime priorStart = now.minusDays(2L * WINDOW_DAYS);

        long freeToPro = planChanges.countDistinctUsersByTransitionSince(
                Plan.FREE, Plan.PRO, windowStart);
        long entitledPro = subscriptions.countEntitledOn(Plan.PRO);
        long entitledBusiness = subscriptions.countEntitledOn(Plan.BUSINESS);

        long priorFreeToPro = planChanges.countDistinctUsersByTransitionBetween(
                Plan.FREE, Plan.PRO, priorStart, windowStart);

        return new ProAdoptionMetrics(
                WINDOW_DAYS,
                freeToPro,
                entitledPro,
                entitledBusiness,
                priorFreeToPro);
    }
}
