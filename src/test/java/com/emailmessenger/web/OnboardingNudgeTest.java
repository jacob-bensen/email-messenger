package com.emailmessenger.web;

import com.emailmessenger.domain.Plan;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingNudgeTest {

    @Test
    void freeUserWithNoStepsBeyondMailboxGetsNoNudge() {
        OnboardingChecklist before = new OnboardingChecklist(true, 4L, false, false);
        assertThat(OnboardingNudge.from(Plan.FREE, before)).isEmpty();
    }

    @Test
    void freeUserPastTenThreadsGetsPersonalThreadCapNudge() {
        OnboardingChecklist after = new OnboardingChecklist(true, 12L, false, false);

        OnboardingNudge nudge = OnboardingNudge.from(Plan.FREE, after).orElseThrow();

        assertThat(nudge.trigger()).isEqualTo("step2");
        assertThat(nudge.upgradeTarget()).isEqualTo(Plan.PERSONAL);
        assertThat(nudge.upgradeTargetParam()).isEqualTo("personal");
        assertThat(nudge.headline()).contains("500 threads");
        assertThat(nudge.ctaLabel()).isEqualTo("Upgrade to Personal — $9/mo");
    }

    @Test
    void freeUserAfterSavedSearchGetsPersonalSavedSearchNudge() {
        OnboardingChecklist after = new OnboardingChecklist(true, 12L, true, false);

        OnboardingNudge nudge = OnboardingNudge.from(Plan.FREE, after).orElseThrow();

        assertThat(nudge.trigger()).isEqualTo("step3");
        assertThat(nudge.upgradeTarget()).isEqualTo(Plan.PERSONAL);
        assertThat(nudge.headline()).contains("1 saved search");
        assertThat(nudge.body()).contains("unlimited saved searches");
        assertThat(nudge.ctaLabel()).isEqualTo("Upgrade to Personal — $9/mo");
    }

    @Test
    void freeUserAfterTeammateInvitedGetsTeamPlanNudge() {
        OnboardingChecklist after = new OnboardingChecklist(true, 25L, true, true);

        OnboardingNudge nudge = OnboardingNudge.from(Plan.FREE, after).orElseThrow();

        assertThat(nudge.trigger()).isEqualTo("step4");
        assertThat(nudge.upgradeTarget()).isEqualTo(Plan.TEAM);
        assertThat(nudge.upgradeTargetParam()).isEqualTo("team");
        assertThat(nudge.headline()).contains("Team plan");
        assertThat(nudge.body()).contains("shared threads");
        assertThat(nudge.ctaLabel()).isEqualTo("Upgrade to Team — $29/mo");
    }

    @Test
    void teammateInvitedTakesPrecedenceOverEarlierSteps() {
        // All four step flags trip — the latest, highest-value trigger wins
        // so the Free user sees the Team plan upsell once they've started
        // inviting (the step that gates the Team-only sharing feature),
        // not the earlier Personal-tier framing.
        OnboardingChecklist after = new OnboardingChecklist(true, 50L, true, true);

        OnboardingNudge nudge = OnboardingNudge.from(Plan.FREE, after).orElseThrow();

        assertThat(nudge.trigger()).isEqualTo("step4");
        assertThat(nudge.upgradeTarget()).isEqualTo(Plan.TEAM);
    }

    @Test
    void personalPlanGetsNoNudgeNoMatterWhatStateTheyAreIn() {
        for (OnboardingChecklist c : new OnboardingChecklist[] {
                new OnboardingChecklist(true, 12L, false, false),
                new OnboardingChecklist(true, 12L, true, false),
                new OnboardingChecklist(true, 25L, true, true)
        }) {
            assertThat(OnboardingNudge.from(Plan.PERSONAL, c)).isEmpty();
        }
    }

    @Test
    void teamAndEnterprisePlansAlsoGetNoNudge() {
        OnboardingChecklist activated = new OnboardingChecklist(true, 25L, true, true);
        assertThat(OnboardingNudge.from(Plan.TEAM, activated)).isEmpty();
        assertThat(OnboardingNudge.from(Plan.ENTERPRISE, activated)).isEmpty();
    }

    @Test
    void freeUserWithEmptyChecklistGetsNoNudge() {
        OnboardingChecklist fresh = new OnboardingChecklist(false, 0L, false, false);
        assertThat(OnboardingNudge.from(Plan.FREE, fresh)).isEmpty();
    }

    @Test
    void freshFreeUserWithMailboxButFewThreadsGetsNoNudgeYet() {
        // 7 threads — below the 10-thread threshold for step 2, so the nudge
        // doesn't fire until the user has felt enough of the chat-view payoff
        // to be receptive to an upsell.
        OnboardingChecklist early = new OnboardingChecklist(true, 7L, false, false);
        Optional<OnboardingNudge> nudge = OnboardingNudge.from(Plan.FREE, early);
        assertThat(nudge).isEmpty();
    }
}
