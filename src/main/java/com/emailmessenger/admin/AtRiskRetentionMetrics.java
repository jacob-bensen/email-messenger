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
 *
 * <p>EPIC-18 M1 adds {@code subscriptionId} so the per-row "Send win-back"
 * form has a target, and {@code winBackSentAt} so a row that already had
 * an outreach fired renders the timestamp instead of the action button.
 *
 * <p>EPIC-18 M3 widens the queue to also surface <em>recovered</em> rows —
 * subs that received a win-back email and have since flipped back to
 * {@code active}. The row keeps its spot for context (so the operator can
 * tell on the same page whether a click paid off) but renders a
 * "Recovered" badge in place of the action cell.
 */
public record AtRiskRetentionMetrics(int windowDays,
                                     long totalCanceledInWindow,
                                     long totalRecoveredInWindow,
                                     int displayLimit,
                                     List<Entry> entries) {

    public boolean isTruncated() {
        return (totalCanceledInWindow + totalRecoveredInWindow) > entries.size();
    }

    public long totalInWindow() {
        return totalCanceledInWindow + totalRecoveredInWindow;
    }

    public record Entry(Long subscriptionId,
                        String customerEmail,
                        String planLabel,
                        String cadenceLabel,
                        long monthlyEquivalentCents,
                        String monthlyEquivalentFormatted,
                        String sourceLabel,
                        String reasonLabel,
                        LocalDateTime canceledAt,
                        LocalDateTime winBackSentAt,
                        boolean recovered) {

        public boolean winBackAlreadySent() {
            return winBackSentAt != null;
        }
    }

    public static AtRiskRetentionMetrics empty(int windowDays, int displayLimit) {
        return new AtRiskRetentionMetrics(windowDays, 0, 0, displayLimit, List.of());
    }
}
