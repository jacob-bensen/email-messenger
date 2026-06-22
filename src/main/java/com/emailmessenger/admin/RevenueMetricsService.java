package com.emailmessenger.admin;

import com.emailmessenger.admin.RevenueMetrics.PlanBreakdown;
import com.emailmessenger.admin.RevenueMetrics.RecentSubscriptionEvent;
import com.emailmessenger.admin.RevenueMetrics.SourceBreakdown;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RevenueMetricsService {

    static final int RECENT_EVENT_LIMIT = 10;
    static final int TRIAL_ENDING_SOON_DAYS = 7;
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_TRIALING = "trialing";
    private static final String STATUS_CANCELED = "canceled";

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    RevenueMetricsService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RevenueMetrics snapshot() {
        List<Subscription> all = subscriptions.findAllWithUserNewestFirst();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime trialCutoff = now.plusDays(TRIAL_ENDING_SOON_DAYS);

        long mrrCents = 0;
        long trialPipelineCents = 0;
        int activeCount = 0;
        int trialingCount = 0;
        int canceledCount = 0;
        int monthlyActive = 0;
        int annualActive = 0;
        int trialsEndingSoon = 0;

        EnumMap<Plan, int[]> planMonthlyAnnual = new EnumMap<>(Plan.class);
        EnumMap<Plan, long[]> planMrr = new EnumMap<>(Plan.class);
        Map<String, int[]> sourceActive = new LinkedHashMap<>();
        Map<String, long[]> sourceMrr = new LinkedHashMap<>();

        for (Subscription sub : all) {
            Plan plan = sub.getPlan();
            if (plan == null || plan == Plan.FREE) {
                continue;
            }
            String status = sub.getStatus() == null ? "" : sub.getStatus().toLowerCase();
            BillingPeriod period = sub.getBillingPeriod() == null
                    ? BillingPeriod.MONTHLY
                    : sub.getBillingPeriod();
            int priceCents = PlanPricing.monthlyCents(plan, period);

            switch (status) {
                case STATUS_ACTIVE -> {
                    activeCount++;
                    if (period == BillingPeriod.ANNUAL) {
                        annualActive++;
                    } else {
                        monthlyActive++;
                    }
                    mrrCents += priceCents;
                    int[] pma = planMonthlyAnnual.computeIfAbsent(plan, p -> new int[2]);
                    if (period == BillingPeriod.ANNUAL) pma[1]++;
                    else pma[0]++;
                    planMrr.computeIfAbsent(plan, p -> new long[1])[0] += priceCents;
                    String source = normaliseSource(sub.getUser().getAcquisitionSource());
                    sourceActive.computeIfAbsent(source, s -> new int[1])[0]++;
                    sourceMrr.computeIfAbsent(source, s -> new long[1])[0] += priceCents;
                }
                case STATUS_TRIALING -> {
                    trialingCount++;
                    trialPipelineCents += priceCents;
                    if (sub.getTrialEndsAt() != null && sub.getTrialEndsAt().isBefore(trialCutoff)) {
                        trialsEndingSoon++;
                    }
                }
                case STATUS_CANCELED -> canceledCount++;
                default -> {
                    // incomplete / past_due / unpaid don't count toward MRR or trial pipeline.
                }
            }
        }

        int totalActive = monthlyActive + annualActive;
        int annualSharePercent = totalActive == 0 ? 0 : (int) Math.round(100.0 * annualActive / totalActive);

        List<PlanBreakdown> planBreakdown = new ArrayList<>();
        for (Plan plan : List.of(Plan.PRO, Plan.BUSINESS)) {
            int[] pma = planMonthlyAnnual.getOrDefault(plan, new int[2]);
            long planMrrCents = planMrr.getOrDefault(plan, new long[1])[0];
            planBreakdown.add(new PlanBreakdown(planLabel(plan), pma[0], pma[1],
                    planMrrCents, formatCents(planMrrCents)));
        }

        List<SourceBreakdown> sourceBreakdown = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : sourceActive.entrySet()) {
            long srcMrr = sourceMrr.get(entry.getKey())[0];
            sourceBreakdown.add(new SourceBreakdown(entry.getKey(),
                    entry.getValue()[0], srcMrr, formatCents(srcMrr)));
        }
        sourceBreakdown.sort(Comparator.comparingLong(SourceBreakdown::mrrCents).reversed());

        List<RecentSubscriptionEvent> recentEvents = new ArrayList<>();
        for (Subscription sub : all) {
            if (recentEvents.size() >= RECENT_EVENT_LIMIT) {
                break;
            }
            if (sub.getPlan() == null || sub.getPlan() == Plan.FREE) {
                continue;
            }
            recentEvents.add(new RecentSubscriptionEvent(
                    sub.getUser().getEmail(),
                    planLabel(sub.getPlan()),
                    periodLabel(sub.getBillingPeriod()),
                    sub.getStatus(),
                    sub.getUpdatedAt()));
        }

        long arrCents = mrrCents * 12L;
        return new RevenueMetrics(
                mrrCents, formatCents(mrrCents),
                arrCents, formatCents(arrCents),
                trialPipelineCents, formatCents(trialPipelineCents),
                activeCount, trialingCount, canceledCount,
                monthlyActive, annualActive, annualSharePercent,
                trialsEndingSoon,
                planBreakdown, sourceBreakdown, recentEvents);
    }

    private static String normaliseSource(String source) {
        if (source == null || source.isBlank()) {
            return "Direct / unknown";
        }
        return source;
    }

    private static String planLabel(Plan plan) {
        return switch (plan) {
            case PRO -> "Pro";
            case BUSINESS -> "Business";
            case FREE -> "Free";
        };
    }

    private static String periodLabel(BillingPeriod period) {
        return period == BillingPeriod.ANNUAL ? "Annual" : "Monthly";
    }

    static String formatCents(long cents) {
        long dollars = cents / 100;
        long remainder = cents % 100;
        StringBuilder sb = new StringBuilder().append('$');
        String digits = Long.toString(dollars);
        int firstGroup = digits.length() % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        sb.append(digits, 0, firstGroup);
        for (int i = firstGroup; i < digits.length(); i += 3) {
            sb.append(',').append(digits, i, i + 3);
        }
        sb.append('.');
        if (remainder < 10) {
            sb.append('0');
        }
        sb.append(remainder);
        return sb.toString();
    }
}
