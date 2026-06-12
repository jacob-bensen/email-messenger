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
        if (checklist.teammateInvited()) {
            return Optional.of(new OnboardingNudge(
                    Plan.TEAM,
                    "Sharing your inbox is on the Team plan",
                    "Free includes 1 mailbox and no shared threads. Team adds 10 mailboxes, shared threads with your invitees, and unlimited saved searches.",
                    "Upgrade to Team — $29/mo",
                    "step4"));
        }
        if (checklist.savedSearchSaved()) {
            return Optional.of(new OnboardingNudge(
                    Plan.PERSONAL,
                    "Free includes 1 saved search",
                    "Personal is unlimited saved searches, unlimited threads, and 3 mailboxes — pin every \"everything from X\" view you care about.",
                    "Upgrade to Personal — $9/mo",
                    "step3"));
        }
        if (checklist.threadsImported()) {
            return Optional.of(new OnboardingNudge(
                    Plan.PERSONAL,
                    "Free caps at 500 threads",
                    "Personal removes the thread cap, lifts mailboxes to 3, and unlocks unlimited saved searches and history — keep importing without losing a message.",
                    "Upgrade to Personal — $9/mo",
                    "step2"));
        }
        return Optional.empty();
    }
}
