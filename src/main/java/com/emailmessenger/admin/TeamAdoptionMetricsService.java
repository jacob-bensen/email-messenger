package com.emailmessenger.admin;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.repository.PlanChangeEventRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * "Team-plan adoption — last 30 days" card for {@code /admin/revenue}.
 *
 * <p>Reports plan-transition conversions logged by
 * {@link com.emailmessenger.billing.BillingService} into
 * {@code plan_change_events}, bucketed by from-plan, plus the count of
 * subscribers currently entitled to Team/Enterprise. Each window-scoped
 * metric is also computed for the prior 30-day window so the operator can see
 * the conversion lift against the preceding baseline.
 */
@Service
public class TeamAdoptionMetricsService {

    static final int WINDOW_DAYS = 30;

    private final PlanChangeEventRepository planChanges;
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    TeamAdoptionMetricsService(PlanChangeEventRepository planChanges,
                               SubscriptionRepository subscriptions,
                               Clock clock) {
        this.planChanges = planChanges;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TeamAdoptionMetrics snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);
        LocalDateTime priorStart = now.minusDays(2L * WINDOW_DAYS);

        long freeToTeam = planChanges.countDistinctUsersByTransitionSince(
                Plan.FREE, Plan.TEAM, windowStart);
        long personalToTeam = planChanges.countDistinctUsersByTransitionSince(
                Plan.PERSONAL, Plan.TEAM, windowStart);
        long entitledTeam = subscriptions.countEntitledOn(Plan.TEAM);
        long entitledEnterprise = subscriptions.countEntitledOn(Plan.ENTERPRISE);

        long priorFreeToTeam = planChanges.countDistinctUsersByTransitionBetween(
                Plan.FREE, Plan.TEAM, priorStart, windowStart);
        long priorPersonalToTeam = planChanges.countDistinctUsersByTransitionBetween(
                Plan.PERSONAL, Plan.TEAM, priorStart, windowStart);

        return new TeamAdoptionMetrics(
                WINDOW_DAYS,
                freeToTeam,
                personalToTeam,
                entitledTeam,
                entitledEnterprise,
                priorFreeToTeam,
                priorPersonalToTeam);
    }
}
