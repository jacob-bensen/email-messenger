package com.emailmessenger.admin;

/**
 * Rolling-30-day Pro-plan adoption snapshot for {@code /admin/revenue}.
 *
 * <p>Tracks whether the upgrade pressure is landing: how many Free→Pro
 * conversions did we see in the window, and how many subscribers are currently
 * entitled to Pro/Business. The "prior-window" counterpart carries the same
 * conversion count over the 30 days <em>before</em> the current window so the
 * operator can read the lift directly off the card.
 */
public record ProAdoptionMetrics(
        int windowDays,
        long freeToProConversions,
        long entitledProSubscribers,
        long entitledBusinessSubscribers,
        long priorFreeToProConversions) {

    public int freeToProDeltaPercent() {
        return deltaPercent(freeToProConversions, priorFreeToProConversions);
    }

    public String freeToProDeltaLabel() {
        return deltaLabel(freeToProConversions, priorFreeToProConversions, freeToProDeltaPercent());
    }

    public static ProAdoptionMetrics empty(int windowDays) {
        return new ProAdoptionMetrics(windowDays, 0, 0, 0, 0);
    }

    private static int deltaPercent(long current, long prior) {
        if (prior <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * (current - prior) / prior);
    }

    // A current value with no prior baseline reads as "new" rather than a
    // misleading "0% — flat", and a current zero against a prior zero is
    // genuinely flat. Splitting these cases is the only reason this isn't
    // a raw percentage string in the template.
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
