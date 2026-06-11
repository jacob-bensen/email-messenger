package com.emailmessenger.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rolling-30-day "who do I email to win back" list rendered on
 * {@code /admin/revenue}. EPIC-17 M3.
 *
 * <p>Each {@link Entry} is one row that flipped {@code active → canceled} in
 * the current window, surfaced with the legible context the operator needs
 * to compose a win-back email: customer address, plan + cadence dollar
 * weight, the acquisition source that brought them in, and the
 * cancellation reason they picked at the point of cancel (if EPIC-17 M2's
 * reason picker captured one — direct Stripe-Portal cancels stay
 * {@code "not recorded"} rather than being guessed).
 */
public record AtRiskRetentionMetrics(int windowDays,
                                     long totalCanceledInWindow,
                                     int displayLimit,
                                     List<Entry> entries) {

    public boolean isTruncated() {
        return totalCanceledInWindow > entries.size();
    }

    public record Entry(String customerEmail,
                        String planLabel,
                        String cadenceLabel,
                        long monthlyEquivalentCents,
                        String monthlyEquivalentFormatted,
                        String sourceLabel,
                        String reasonLabel,
                        LocalDateTime canceledAt) {}

    public static AtRiskRetentionMetrics empty(int windowDays, int displayLimit) {
        return new AtRiskRetentionMetrics(windowDays, 0, displayLimit, List.of());
    }
}
