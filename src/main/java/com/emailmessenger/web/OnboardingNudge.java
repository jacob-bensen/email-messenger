package com.emailmessenger.web;

import com.emailmessenger.domain.Plan;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

/**
 * View model for the per-step upgrade nudge that lives inside the onboarding
 * progress card on /threads. Free users only — paid plans get no nudge.
 *
 * Each nudge ties the user's most-recently-completed activation step to the
 * plan that removes the cap that step's feature surface is about to bump
 * into. Posting the {@link #upgradeTargetParam()} value to
 * {@code /billing/checkout} reuses the same Stripe entry point as the
 * inline {@link com.emailmessenger.billing.UpgradeModal}.
 */
public record OnboardingNudge(
        Plan upgradeTarget,
        String headline,
        String body,
        String ctaLabel,
        String trigger
) implements Serializable {

    public String upgradeTargetParam() {
        return upgradeTarget.name().toLowerCase(Locale.ROOT);
    }

    public static Optional<OnboardingNudge> from(Plan currentPlan, OnboardingChecklist checklist) {
        if (currentPlan != Plan.FREE) {
            return Optional.empty();
        }
        if (checklist.threadsImported()) {
            return Optional.of(new OnboardingNudge(
                    Plan.PRO,
                    "Free caps at 500 threads",
                    "Pro removes the thread cap for full history and lifts mailboxes from 3 to 5 — keep importing without losing a message.",
                    "Upgrade to Pro — $6.99/mo",
                    "step2"));
        }
        return Optional.empty();
    }
}
