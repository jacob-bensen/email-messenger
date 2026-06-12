package com.emailmessenger.admin;

/**
 * Rolling-30-day onboarding funnel for {@code /admin/revenue}: how many of
 * this month's signups crossed each activation step on the way to a paid
 * subscription. Anchored on {@code users.created_at} so a signup that
 * happened pre-window but converted this week does not inflate any column.
 * Surfacing the per-step drop-off lets the operator see which onboarding
 * step is the next monetization leak — the whole point of EPIC-15 M4.
 */
public record OnboardingFunnelMetrics(
        int windowDays,
        int signups,
        int mailboxConnected,
        int tenThreadsImported,
        int savedSearchSaved,
        int inviteSent,
        int paid,
        int mailboxRatePercent,
        int tenThreadsRatePercent,
        int savedSearchRatePercent,
        int inviteRatePercent,
        int paidRatePercent) {

    public static OnboardingFunnelMetrics empty(int windowDays) {
        return new OnboardingFunnelMetrics(windowDays, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
