package com.emailmessenger.admin;

import com.emailmessenger.admin.ChurnMetrics.PlanChurnBreakdown;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Computes the rolling-30-day cancellation + lost-MRR snapshot rendered on
 * the operator dashboard's "Churn" card. Reuses {@link PlanPricing} so the
 * lost-revenue figures match the live MRR card to the cent.
 */
@Service
public class ChurnMetricsService {

    static final int WINDOW_DAYS = 30;
    private static final String STATUS_ACTIVE = "active";

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    ChurnMetricsService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ChurnMetrics snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);
        LocalDateTime priorStart = now.minusDays(2L * WINDOW_DAYS);

        long currentMrrCents = sumActiveMrr();

        List<Subscription> windowCancels = subscriptions.findCanceledBetween(windowStart, now);
        EnumMap<Plan, long[]> perPlan = new EnumMap<>(Plan.class);
        long canceledSubs = 0;
        long lostMrrCents = 0;
        for (Subscription sub : windowCancels) {
            Plan plan = sub.getPlan();
            if (plan == null || plan == Plan.FREE) {
                continue;
            }
            int priceCents = PlanPricing.monthlyCents(plan, periodOf(sub));
            canceledSubs++;
            lostMrrCents += priceCents;
            long[] row = perPlan.computeIfAbsent(plan, p -> new long[2]);
            row[0]++;
            row[1] += priceCents;
        }

        long priorCanceledSubs = 0;
        long priorLostMrrCents = 0;
        for (Subscription sub : subscriptions.findCanceledBetween(priorStart, windowStart)) {
            Plan plan = sub.getPlan();
            if (plan == null || plan == Plan.FREE) {
                continue;
            }
            priorCanceledSubs++;
            priorLostMrrCents += PlanPricing.monthlyCents(plan, periodOf(sub));
        }

        long startingMrrCents = currentMrrCents + lostMrrCents;
        long priorStartingMrrCents = currentMrrCents + lostMrrCents + priorLostMrrCents;
        int churnRate = ratePercent(lostMrrCents, startingMrrCents);
        int priorChurnRate = ratePercent(priorLostMrrCents, priorStartingMrrCents);

        List<PlanChurnBreakdown> byPlan = new ArrayList<>();
        for (Plan plan : List.of(Plan.PERSONAL, Plan.TEAM, Plan.ENTERPRISE)) {
            long[] row = perPlan.getOrDefault(plan, new long[2]);
            byPlan.add(new PlanChurnBreakdown(
                    planLabel(plan), row[0], row[1],
                    RevenueMetricsService.formatCents(row[1])));
        }

        return new ChurnMetrics(
                WINDOW_DAYS,
                canceledSubs,
                lostMrrCents, RevenueMetricsService.formatCents(lostMrrCents),
                lostMrrCents * 12L, RevenueMetricsService.formatCents(lostMrrCents * 12L),
                startingMrrCents, RevenueMetricsService.formatCents(startingMrrCents),
                churnRate,
                byPlan,
                priorCanceledSubs,
                priorLostMrrCents, RevenueMetricsService.formatCents(priorLostMrrCents),
                priorChurnRate);
    }

    private long sumActiveMrr() {
        long mrr = 0;
        for (Subscription sub : subscriptions.findAllWithUserNewestFirst()) {
            if (sub.getPlan() == null || sub.getPlan() == Plan.FREE) {
                continue;
            }
            if (!STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus())) {
                continue;
            }
            mrr += PlanPricing.monthlyCents(sub.getPlan(), periodOf(sub));
        }
        return mrr;
    }

    private static BillingPeriod periodOf(Subscription sub) {
        return sub.getBillingPeriod() == null ? BillingPeriod.MONTHLY : sub.getBillingPeriod();
    }

    private static int ratePercent(long lost, long starting) {
        if (starting <= 0 || lost <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * lost / starting);
    }

    private static String planLabel(Plan plan) {
        return switch (plan) {
            case PERSONAL -> "Personal";
            case TEAM -> "Team";
            case ENTERPRISE -> "Enterprise";
            case FREE -> "Free";
        };
    }
}
