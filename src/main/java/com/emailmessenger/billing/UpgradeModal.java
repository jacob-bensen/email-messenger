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
 */
public record UpgradeModal(
        Plan currentPlan,
        PlanLimitKind kind,
        long limit,
        long current,
        Plan upgradeTarget
) implements Serializable {

    public static UpgradeModal fromException(PlanLimitExceededException ex) {
        return new UpgradeModal(
                ex.getCurrentPlan(),
                ex.getKind(),
                ex.getLimit(),
                ex.getCurrent(),
                Plan.PERSONAL);
    }

    /** Lowercase identifier accepted by {@code POST /billing/checkout?plan=…}. */
    public String upgradeTargetParam() {
        return upgradeTarget.name().toLowerCase(Locale.ROOT);
    }
}
