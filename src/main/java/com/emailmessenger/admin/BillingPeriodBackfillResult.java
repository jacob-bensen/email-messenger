package com.emailmessenger.admin;

/**
 * Outcome of one "Reconcile from Stripe" run over rows whose local
 * {@code billing_period} is NULL. {@link #scanned} is the candidate count;
 * the rest partition those candidates so the operator can tell the run
 * apart from a no-op (one of {@link #updated}, {@link #missingStripeId},
 * {@link #unmatchedPriceId}, {@link #stripeMisses} accounts for each).
 */
public record BillingPeriodBackfillResult(
        int scanned,
        int updated,
        int missingStripeId,
        int unmatchedPriceId,
        int stripeMisses) {

    public static BillingPeriodBackfillResult empty() {
        return new BillingPeriodBackfillResult(0, 0, 0, 0, 0);
    }

    public boolean isNoOp() {
        return scanned == 0;
    }
}
