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
    void freeTierThreadAndSavedSearchCapsUnchanged() {
        PlanLimits free = PlanLimits.forPlan(Plan.FREE);
        assertThat(free.threads()).isEqualTo(500);
        assertThat(free.savedSearches()).isEqualTo(1);
    }

    @Test
    void enterpriseIsUnlimited() {
        PlanLimits ent = PlanLimits.forPlan(Plan.ENTERPRISE);
        assertThat(ent.mailboxes()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(ent.threads()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(ent.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }
}
