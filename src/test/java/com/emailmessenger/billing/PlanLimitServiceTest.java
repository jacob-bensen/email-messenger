package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ConexusMail unlocks every paid feature for every account, so entitlements are
 * always the top tier and the per-plan caps never bite. These tests pin that
 * behavior (the old free-tier paywall assertions were removed with the paywall).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PlanLimitServiceTest {

    @Autowired PlanLimitService planLimitService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean StripeCheckoutGateway gateway;
    @MockBean StripePortalGateway portalGateway;
    @MockBean ReplyService replyService;

    private User newUser(String email) {
        userService.register(email, "password1", "Test");
        return users.findByEmail(email).orElseThrow();
    }

    @Test
    void accountWithoutSubscriptionIsEnterprise() {
        User user = newUser("nosub@test.com");
        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.ENTERPRISE);
    }

    @Test
    void canceledSubscriptionStillGetsEverything() {
        User user = newUser("canceled@test.com");
        Subscription sub = new Subscription(user, "cus_x", "canceled");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.ENTERPRISE);
    }

    @Test
    void limitsAreUnlimitedForEveryone() {
        User user = newUser("limits@test.com");
        PlanLimits caps = planLimitService.limitsFor(user);
        assertThat(caps.threads()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(caps.mailboxes()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(caps.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }

    @Test
    void enforceMethodsNeverThrow() {
        User user = newUser("enforce@test.com");
        assertThatCode(() -> {
            planLimitService.enforceCanCreateThread(user);
            planLimitService.enforceCanCreateMailbox(user);
            planLimitService.enforceCanCreateSavedSearch(user);
        }).doesNotThrowAnyException();
    }
}
