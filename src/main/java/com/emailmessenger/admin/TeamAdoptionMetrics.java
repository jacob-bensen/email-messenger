package com.emailmessenger.admin;

/**
 * Rolling-30-day Team-plan adoption snapshot for {@code /admin/revenue}.
 *
 * <p>EPIC-16 M4. The card answers two questions the operator needs after
 * shipping the shared-inbox surface: (1) is the Team plan actually being
 * <em>used</em> daily — note authors, notes posted, @-mentions — and
 * (2) is the upgrade pressure landing — how many Free→Team and
 * Personal→Team conversions did we see in the window. The split between
 * the two transition paths is the legible "lift" signal: a flat Free→Team
 * with rising Personal→Team means existing payers are climbing tiers; the
 * reverse means top-of-funnel acquisition is doing the work.
 */
public record TeamAdoptionMetrics(
        int windowDays,
        long notesPosted,
        long activeNoteAuthors,
        long teamsWithNotes,
        long mentionsWritten,
        long freeToTeamConversions,
        long personalToTeamConversions,
        long entitledTeamSubscribers,
        long entitledEnterpriseSubscribers) {

    public long totalTeamConversions() {
        return freeToTeamConversions + personalToTeamConversions;
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

    public static TeamAdoptionMetrics empty(int windowDays) {
        return new TeamAdoptionMetrics(windowDays, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
