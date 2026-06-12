package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;

/**
 * Per-plan monthly-equivalent pricing in US cents, mirroring the figures
 * displayed on {@code /pricing}. ANNUAL is the per-month rate when paid
 * annually (~17% lower), not the annual cash total — the dashboard
 * computes MRR, so it always sums in monthly-equivalent cents and then
 * multiplies by 12 for ARR.
 */
final class PlanPricing {

    private PlanPricing() {}

    static int monthlyCents(Plan plan, BillingPeriod period) {
        if (plan == null) {
            return 0;
        }
        if (period == BillingPeriod.ANNUAL) {
            return switch (plan) {
                case PERSONAL -> 700;
                case TEAM -> 2400;
                case ENTERPRISE -> 8300;
                case FREE -> 0;
            };
        }
        return switch (plan) {
            case PERSONAL -> 900;
            case TEAM -> 2900;
            case ENTERPRISE -> 9900;
            case FREE -> 0;
        };
    }
}
