package com.emailmessenger.admin;

/**
 * Rolling-30-day attribution slice for the trial-end conversion email.
 * Surfaces three numbers on {@code /admin/revenue}: how many trial-end
 * nudges went out, how many of those subscriptions are currently
 * {@code active}, and the resulting conversion percentage. Anchored on
 * the {@code last_trial_end_email_sent_at} stamp written by
 * {@link com.emailmessenger.billing.TrialEndConversionService} so the
 * counts only ever reflect outbound activity from this milestone, not
 * any pre-EPIC-14 baseline.
 */
public record TrialEndConversionMetrics(
        int windowDays,
        int emailsSent,
        int converted,
        int conversionRatePercent) {}
