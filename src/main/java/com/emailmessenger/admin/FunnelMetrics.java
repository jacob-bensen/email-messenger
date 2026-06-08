package com.emailmessenger.admin;

import java.util.List;

/**
 * 30-day rolling signup → trial → paid funnel for the operator dashboard,
 * plus a per-{@code acquisition_source} breakdown so the operator can see
 * which channels actually convert. Trial-start and paid-conversion
 * cohorts are anchored on the subscription's {@code created_at} so the
 * paid-conversion rate is a true within-cohort ratio rather than a noisy
 * "any active sub touched this month" count.
 */
public record FunnelMetrics(
        int windowDays,
        int signupsLast30d,
        int trialStartsLast30d,
        int paidConversionsLast30d,
        int trialRatePercent,
        int paidRatePercent,
        List<SourceFunnel> bySource) {

    /** One row of the per-acquisition-source funnel table. */
    public record SourceFunnel(String sourceLabel,
                               int signups,
                               int trialStarts,
                               int paidConversions,
                               int trialRatePercent,
                               int paidRatePercent) {}
}
