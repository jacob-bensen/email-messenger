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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Entitlements come off the user's Stripe subscription: an active/trialing
 * Pro or Business subscription unlocks that tier, while everyone else (no
 * subscription, or an incomplete/canceled one) falls back to Free and its
 * mailbox + history caps.
 */
@SpringBootTest
@Transactional
class PlanLimitServiceTest {

    @Autowired PlanLimitService planLimitService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;

    @MockitoBean StripeCheckoutGateway gateway;
    @MockitoBean StripePortalGateway portalGateway;
    @MockitoBean ReplyService replyService;

    private User newUser(String email) {
        userService.register(email, "password1", "Test");
        return users.findByEmail(email).orElseThrow();
    }

    @Test
    void accountWithoutSubscriptionIsFree() {
        User user = newUser("nosub@test.com");
        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.FREE);
    }

    @Test
    void activeProSubscriptionEntitlesToPro() {
        User user = newUser("pro@test.com");
        Subscription sub = new Subscription(user, "cus_pro", "active");
        sub.setPlan(Plan.PRO);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.PRO);
    }

    @Test
    void trialingProSubscriptionEntitlesToPro() {
        User user = newUser("trial@test.com");
        Subscription sub = new Subscription(user, "cus_trial", "trialing");
        sub.setPlan(Plan.PRO);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.PRO);
    }

    @Test
    void canceledSubscriptionFallsBackToFree() {
        User user = newUser("canceled@test.com");
        Subscription sub = new Subscription(user, "cus_x", "canceled");
        sub.setPlan(Plan.PRO);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.FREE);
    }

    @Test
    void incompleteSubscriptionFallsBackToFree() {
        User user = newUser("incomplete@test.com");
        Subscription sub = new Subscription(user, "cus_i", "incomplete");
        sub.setPlan(Plan.PRO);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.FREE);
    }

    @Test
    void freeUserGetsFreeTierCaps() {
        User user = newUser("limits@test.com");
        PlanLimits caps = planLimitService.limitsFor(user);
        assertThat(caps.threads()).isEqualTo(500);
        assertThat(caps.mailboxes()).isEqualTo(3);
        assertThat(caps.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }

    @Test
    void proUserGetsProTierCaps() {
        User user = newUser("prolimits@test.com");
        Subscription sub = new Subscription(user, "cus_prol", "active");
        sub.setPlan(Plan.PRO);
        subscriptions.save(sub);

        PlanLimits caps = planLimitService.limitsFor(user);
        assertThat(caps.threads()).isEqualTo(PlanLimits.UNLIMITED);
        assertThat(caps.mailboxes()).isEqualTo(5);
        assertThat(caps.savedSearches()).isEqualTo(PlanLimits.UNLIMITED);
    }

    @Test
    void savedSearchGuardNeverThrowsBecauseSavedSearchesAreUnlimited() {
        User user = newUser("enforce@test.com");
        assertThatCode(() -> planLimitService.enforceCanCreateSavedSearch(user))
                .doesNotThrowAnyException();
    }
}
