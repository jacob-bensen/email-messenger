package com.emailmessenger.admin;

import java.util.List;

/**
 * Rolling-30-day cancellation + MRR-churn snapshot for {@code /admin/revenue}.
 *
 * <p>Pairs raw cancellation counts with the monthly-equivalent revenue that
 * walked out — a Team-plan cancel costs ~3× a Personal-plan cancel, so the
 * count alone hides the dollar impact. {@code grossRevenueChurnRatePercent}
 * follows the standard SaaS convention: lost MRR in the window divided by
 * the MRR that was active at the start of the window
 * ({@code currentMrr + lostMrr}).
 *
 * <p>"Prior-window" counterparts mirror each metric over the 30 days before
 * the current window so the operator can read churn momentum directly off
 * the card (a falling churn rate is the signal that Team-plan retention
 * fixes are landing).
 */
public record ChurnMetrics(
        int windowDays,
        long canceledSubscribers,
        long lostMrrCents,
        String lostMrrFormatted,
        long lostArrCents,
        String lostArrFormatted,
        long startingMrrCents,
        String startingMrrFormatted,
        int grossRevenueChurnRatePercent,
        List<PlanChurnBreakdown> byPlan,
        long priorCanceledSubscribers,
        long priorLostMrrCents,
        String priorLostMrrFormatted,
        int priorGrossRevenueChurnRatePercent) {

    /** Per-plan slice: how many seats lapsed and the lost monthly-equivalent. */
    public record PlanChurnBreakdown(String planLabel,
                                     long canceledSubscribers,
                                     long lostMrrCents,
                                     String lostMrrFormatted) {}

    public int canceledDeltaPercent() {
        return deltaPercent(canceledSubscribers, priorCanceledSubscribers);
    }

    public int lostMrrDeltaPercent() {
        return deltaPercent(lostMrrCents, priorLostMrrCents);
    }

    public int churnRateDeltaPoints() {
        return grossRevenueChurnRatePercent - priorGrossRevenueChurnRatePercent;
    }

    public String canceledDeltaLabel() {
        return deltaLabel(canceledSubscribers, priorCanceledSubscribers, canceledDeltaPercent());
    }

    public String lostMrrDeltaLabel() {
        return deltaLabel(lostMrrCents, priorLostMrrCents, lostMrrDeltaPercent());
    }

    // Churn rate is already a percentage, so the delta is in percentage
    // points, not a percent-of-percent. A flat zero against zero reads as
    // "no prior-window data" so the operator can tell a real flat (we had
    // 4% then 4%) from "nobody churned either window".
    public String churnRateDeltaLabel() {
        if (priorGrossRevenueChurnRatePercent <= 0 && grossRevenueChurnRatePercent <= 0) {
            return "no prior-window data";
        }
        int delta = churnRateDeltaPoints();
        if (delta == 0) {
            return "flat vs. prior 30 days";
        }
        String arrow = delta > 0 ? "▲" : "▼";
        return arrow + " " + Math.abs(delta) + " pts vs. prior 30 days";
    }

    public static ChurnMetrics empty(int windowDays) {
        return new ChurnMetrics(windowDays,
                0, 0, "$0", 0, "$0",
                0, "$0",
                0, List.of(),
                0, 0, "$0", 0);
    }

    private static int deltaPercent(long current, long prior) {
        if (prior <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * (current - prior) / prior);
    }

    private static String deltaLabel(long current, long prior, int percent) {
        if (prior <= 0) {
            if (current <= 0) {
                return "no prior-window data";
            }
            return "new vs. prior 30 days";
        }
        if (percent == 0) {
            return "flat vs. prior 30 days";
        }
        String arrow = percent > 0 ? "▲" : "▼";
        return arrow + " " + Math.abs(percent) + "% vs. prior 30 days";
    }
}
