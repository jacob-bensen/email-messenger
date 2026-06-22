package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;

import java.io.Serializable;
import java.util.Locale;

/**
 * View model for the inline upgrade modal rendered over the thread list when
 * a {@link PlanLimitExceededException} interrupts a controller-driven import.
 * Survives the redirect-after-exception via a flash attribute, so the Free
 * user lands back in their inbox with a plan-comparison panel on top instead
 * of being kicked to a separate error page.
 *
 * Carries the annual price framing alongside the monthly headline so the
 * modal's Monthly|Annual sub-toggle can swap the displayed price + the
 * checkout form's billing-period param without a second round-trip.
 */
public record UpgradeModal(
        Plan currentPlan,
        PlanLimitKind kind,
        long limit,
        long current,
        Plan upgradeTarget,
        String monthlyPrice,
        String annualMonthlyEquivalent,
        String annualCashAmount
) implements Serializable {

    public static UpgradeModal fromException(PlanLimitExceededException ex) {
        Plan target = Plan.PRO;
        return new UpgradeModal(
                ex.getCurrentPlan(),
                ex.getKind(),
                ex.getLimit(),
                ex.getCurrent(),
                target,
                monthlyPrice(target),
                annualMonthlyEquivalent(target),
                annualCashAmount(target));
    }

    /** Lowercase identifier accepted by {@code POST /billing/checkout?plan=…}. */
    public String upgradeTargetParam() {
        return upgradeTarget.name().toLowerCase(Locale.ROOT);
    }

    private static String monthlyPrice(Plan plan) {
        return switch (plan) {
            case PRO -> "$6.99";
            case BUSINESS, FREE -> "";
        };
    }

    private static String annualMonthlyEquivalent(Plan plan) {
        return switch (plan) {
            case PRO -> "$5.99";
            case BUSINESS, FREE -> "";
        };
    }

    private static String annualCashAmount(Plan plan) {
        return switch (plan) {
            case PRO -> "$71.88";
            case BUSINESS, FREE -> "";
        };
    }
}
