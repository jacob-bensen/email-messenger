package com.emailmessenger.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pre-formatted operator dashboard view model. Cents-denominated counters
 * stay numeric for tests; the matching {@code …Formatted} string fields
 * are USD with thousands separators ("$1,234") for direct template render.
 */
public record RevenueMetrics(
        long mrrCents,
        String mrrFormatted,
        long arrCents,
        String arrFormatted,
        long trialPipelineCents,
        String trialPipelineFormatted,
        int activeSubscribers,
        int trialingSubscribers,
        int canceledSubscribers,
        int monthlyActive,
        int annualActive,
        int annualSharePercent,
        int trialsEndingSoon,
        List<PlanBreakdown> planBreakdown,
        List<SourceBreakdown> sourceBreakdown,
        List<RecentSubscriptionEvent> recentEvents) {

    /** Per-plan active-subscriber slice (trialing excluded from MRR). */
    public record PlanBreakdown(String planLabel,
                                int monthlyActive,
                                int annualActive,
                                long mrrCents,
                                String mrrFormatted) {}

    /** Active subscribers grouped by {@code users.acquisition_source}. */
    public record SourceBreakdown(String sourceLabel,
                                  int activeSubscribers,
                                  long mrrCents,
                                  String mrrFormatted) {}

    /** Last N subscriptions touched, regardless of status. */
    public record RecentSubscriptionEvent(String userEmail,
                                          String planLabel,
                                          String periodLabel,
                                          String status,
                                          LocalDateTime at) {}
}
