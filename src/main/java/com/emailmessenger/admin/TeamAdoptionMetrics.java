package com.emailmessenger.admin;

/**
 * Rolling-30-day Team-plan adoption snapshot for {@code /admin/revenue}.
 *
 * <p>Tracks whether the upgrade pressure is landing: how many Free→Team and
 * Personal→Team conversions did we see in the window, and how many subscribers
 * are currently entitled to Team/Enterprise. The split between the two
 * transition paths is the legible "lift" signal: a flat Free→Team with rising
 * Personal→Team means existing payers are climbing tiers; the reverse means
 * top-of-funnel acquisition is doing the work.
 *
 * <p>"Prior-window" counterparts carry the same metric over the 30 days
 * <em>before</em> the current window so the operator can read the conversion
 * lift directly off the card.
 */
public record TeamAdoptionMetrics(
        int windowDays,
        long freeToTeamConversions,
        long personalToTeamConversions,
        long entitledTeamSubscribers,
        long entitledEnterpriseSubscribers,
        long priorFreeToTeamConversions,
        long priorPersonalToTeamConversions) {

    public long totalTeamConversions() {
        return freeToTeamConversions + personalToTeamConversions;
    }

    public long priorTotalTeamConversions() {
        return priorFreeToTeamConversions + priorPersonalToTeamConversions;
    }

    public int freeToTeamSharePercent() {
        long total = totalTeamConversions();
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * freeToTeamConversions / total);
    }

    public int personalToTeamSharePercent() {
        long total = totalTeamConversions();
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * personalToTeamConversions / total);
    }

    public int freeToTeamDeltaPercent() {
        return deltaPercent(freeToTeamConversions, priorFreeToTeamConversions);
    }

    public int personalToTeamDeltaPercent() {
        return deltaPercent(personalToTeamConversions, priorPersonalToTeamConversions);
    }

    public int totalTeamConversionsDeltaPercent() {
        return deltaPercent(totalTeamConversions(), priorTotalTeamConversions());
    }

    public String freeToTeamDeltaLabel() {
        return deltaLabel(freeToTeamConversions, priorFreeToTeamConversions,
                freeToTeamDeltaPercent());
    }

    public String personalToTeamDeltaLabel() {
        return deltaLabel(personalToTeamConversions, priorPersonalToTeamConversions,
                personalToTeamDeltaPercent());
    }

    public String totalTeamConversionsDeltaLabel() {
        return deltaLabel(totalTeamConversions(), priorTotalTeamConversions(),
                totalTeamConversionsDeltaPercent());
    }

    public static TeamAdoptionMetrics empty(int windowDays) {
        return new TeamAdoptionMetrics(windowDays, 0, 0, 0, 0, 0, 0);
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
