package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;

/**
 * Per-plan monthly-equivalent pricing in US cents, mirroring the figures
 * displayed on {@code /pricing}. ANNUAL is the per-month rate when paid
 * annually (~17% lower), not the annual cash total — the dashboard
 * computes MRR, so it always sums in monthly-equivalent cents and then
 * multiplies by 12 for ARR.
 *
 * Public so both the admin MRR calc and the public /api/v1/plans REST
 * endpoint read the same source of truth for pricing figures.
 */
public final class PlanPricing {

    private PlanPricing() {}

    public static int monthlyCents(Plan plan, BillingPeriod period) {
        if (plan == null) {
            return 0;
        }
        // Business is custom / contact-sales — no fixed price, so it
        // contributes nothing to the dashboard's MRR roll-up.
        if (period == BillingPeriod.ANNUAL) {
            return switch (plan) {
                case PRO -> 599;
                case BUSINESS, FREE -> 0;
            };
        }
        return switch (plan) {
            case PRO -> 699;
            case BUSINESS, FREE -> 0;
        };
    }
}
