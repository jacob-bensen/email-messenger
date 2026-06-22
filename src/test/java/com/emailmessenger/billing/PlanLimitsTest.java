package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the per-plan caps. These aren't enforced today (every account is
 * entitled to the top tier), but they define the free tier for if/when the
 * paywall returns — notably the 3-mailbox Free cap.
 */
class PlanLimitsTest {

    @Test
    void freeTierAllowsThreeMailboxes() {
        assertThat(PlanLimits.forPlan(Plan.FREE).mailboxes()).isEqualTo(3);
    }

    @Test
    void freeTierCapsThreadsButAllowsUnlimitedSavedSearches() {
        PlanLimits free = PlanLimits.forPlan(Plan.FREE);
        assertThat(free.threads()).isEqualTo(500);
        assertThat(free.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }

    @Test
    void proLiftsMailboxAndThreadCaps() {
        PlanLimits pro = PlanLimits.forPlan(Plan.PRO);
        assertThat(pro.mailboxes()).isEqualTo(5);
        assertThat(pro.threads()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(pro.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }

    @Test
    void businessIsUnlimited() {
        PlanLimits biz = PlanLimits.forPlan(Plan.BUSINESS);
        assertThat(biz.mailboxes()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(biz.threads()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(biz.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }
}
