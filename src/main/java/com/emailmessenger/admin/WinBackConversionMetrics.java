package com.emailmessenger.admin;

/**
 * Rolling-30-day attribution slice for EPIC-18 operator-initiated win-back
 * outreach: how many "Send win-back" emails went out, how many of those
 * subscriptions are currently {@code active} again, and the monthly-
 * equivalent MRR that walked back through the door. Anchored on the
 * {@code last_win_back_email_sent_at} stamp written by
 * {@link WinBackOutreachService} so the counts only ever reflect outbound
 * activity from this milestone.
 *
 * <p>Mirrors the prior-window delta pattern used by {@link ChurnMetrics} so
 * the operator can read "more / fewer wins than last month" at a glance —
 * an EPIC-17 cancellation isn't fixed by sending the email, only by the
 * customer coming back, and the trend is what tells you whether the
 * win-back copy or the discount offer needs to change.
 */
public record WinBackConversionMetrics(
        int windowDays,
        int emailsSent,
        int reactivated,
        long mrrRecoveredCents,
        String mrrRecoveredFormatted,
        int conversionRatePercent,
        int priorEmailsSent,
        int priorReactivated,
        long priorMrrRecoveredCents,
        String priorMrrRecoveredFormatted,
        int priorConversionRatePercent) {

    public int emailsSentDeltaPercent() {
        return deltaPercent(emailsSent, priorEmailsSent);
    }

    public int reactivatedDeltaPercent() {
        return deltaPercent(reactivated, priorReactivated);
    }

    public int mrrRecoveredDeltaPercent() {
        return deltaPercent(mrrRecoveredCents, priorMrrRecoveredCents);
    }

    public int conversionRateDeltaPoints() {
        return conversionRatePercent - priorConversionRatePercent;
    }

    public String emailsSentDeltaLabel() {
        return deltaLabel(emailsSent, priorEmailsSent, emailsSentDeltaPercent());
    }

    public String reactivatedDeltaLabel() {
        return deltaLabel(reactivated, priorReactivated, reactivatedDeltaPercent());
    }

    public String mrrRecoveredDeltaLabel() {
        return deltaLabel(mrrRecoveredCents, priorMrrRecoveredCents, mrrRecoveredDeltaPercent());
    }

    // Conversion rate is already a percentage, so its delta is in points.
    // A flat zero against zero reads as "no prior-window data" so the
    // operator can tell a real flat (3% then 3%) apart from "nobody got
    // emailed either window yet".
    public String conversionRateDeltaLabel() {
        if (priorConversionRatePercent <= 0 && conversionRatePercent <= 0) {
            return "no prior-window data";
        }
        int delta = conversionRateDeltaPoints();
        if (delta == 0) {
            return "flat vs. prior 30 days";
        }
        String arrow = delta > 0 ? "▲" : "▼";
        return arrow + " " + Math.abs(delta) + " pts vs. prior 30 days";
    }

    public static WinBackConversionMetrics empty(int windowDays) {
        return new WinBackConversionMetrics(windowDays,
                0, 0, 0L, "$0", 0,
                0, 0, 0L, "$0", 0);
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
