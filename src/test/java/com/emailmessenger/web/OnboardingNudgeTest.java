package com.emailmessenger.web;

import com.emailmessenger.domain.Plan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingNudgeTest {

    @Test
    void freeUserPastTheThreadThresholdGetsProThreadCapNudge() {
        OnboardingChecklist after = new OnboardingChecklist(true, 12L, false, false);

        OnboardingNudge nudge = OnboardingNudge.from(Plan.FREE, after).orElseThrow();

        assertThat(nudge.trigger()).isEqualTo("step2");
        assertThat(nudge.upgradeTarget()).isEqualTo(Plan.PRO);
        assertThat(nudge.upgradeTargetParam()).isEqualTo("pro");
        assertThat(nudge.headline()).contains("500 threads");
        assertThat(nudge.body()).contains("3 to 5");
        assertThat(nudge.ctaLabel()).isEqualTo("Upgrade to Pro — $6.99/mo");
    }

    @Test
    void freeUserWithNothingDoneGetsNoNudge() {
        OnboardingChecklist fresh = new OnboardingChecklist(false, 0L, false, false);
        assertThat(OnboardingNudge.from(Plan.FREE, fresh)).isEmpty();
    }

    @Test
    void freeUserWithMailboxButTooFewThreadsGetsNoNudgeYet() {
        // 7 threads — below the 10-thread threshold, so threadsImported() is
        // false and the only nudge doesn't fire.
        OnboardingChecklist early = new OnboardingChecklist(true, 7L, false, false);
        assertThat(OnboardingNudge.from(Plan.FREE, early)).isEmpty();
    }

    @Test
    void proPlanGetsNoNudgeNoMatterWhatStateTheyAreIn() {
        for (OnboardingChecklist c : new OnboardingChecklist[] {
                new OnboardingChecklist(true, 12L, false, false),
                new OnboardingChecklist(true, 12L, true, false),
                new OnboardingChecklist(true, 25L, true, true)
        }) {
            assertThat(OnboardingNudge.from(Plan.PRO, c)).isEmpty();
        }
    }

    @Test
    void businessPlanGetsNoNudge() {
        OnboardingChecklist activated = new OnboardingChecklist(true, 25L, true, true);
        assertThat(OnboardingNudge.from(Plan.BUSINESS, activated)).isEmpty();
    }
}
