package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.PlanChangeEvent;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.PlanChangeEventRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BillingServiceTest {

    @Autowired BillingService billingService;
    @Autowired BillingProperties properties;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired PlanChangeEventRepository planChanges;

    @MockBean StripeCheckoutGateway gateway;
    @MockBean StripePortalGateway portalGateway;
    @MockBean ReplyService replyService;

    @BeforeEach
    void seedPriceIds() {
        properties.setPersonalPriceId("price_personal_test");
        properties.setTeamPriceId("price_team_test");
        properties.setPersonalAnnualPriceId("price_personal_annual_test");
        properties.setTeamAnnualPriceId("price_team_annual_test");
        properties.setEnterpriseAnnualPriceId("");
        properties.setSuccessUrl("http://localhost:8080/billing/success");
        properties.setCancelUrl("http://localhost:8080/billing/cancel");
        properties.setPortalReturnUrl("http://localhost:8080/threads");
        properties.setTrialDays(14);
    }

    private User newUser(String email) {
        userService.register(email, "password1", "Test");
        return users.findByEmail(email).orElseThrow();
    }

    @Test
    void startCheckoutCreatesPendingSubscriptionAndReturnsCheckoutUrl() {
        User user = newUser("alice@example.com");
        when(gateway.createSubscriptionSession(
                eq(null), eq("alice@example.com"), eq("price_personal_test"),
                eq(14), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_test_abc",
                        "cs_test_abc",
                        "cus_test_123"));

        String url = billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        assertThat(url).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_abc");
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripeCustomerId()).isEqualTo("cus_test_123");
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_test");
        assertThat(sub.getPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(sub.getStatus()).isEqualTo("incomplete");
        assertThat(sub.getStripeSubscriptionId()).isNull();
        assertThat(sub.getBillingPeriod()).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void startCheckoutAnnualUsesAnnualPriceId() {
        User user = newUser("annual@example.com");
        when(gateway.createSubscriptionSession(
                eq(null), eq("annual@example.com"), eq("price_personal_annual_test"),
                eq(14), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_test_annual",
                        "cs_test_annual",
                        "cus_test_annual"));

        String url = billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.ANNUAL);

        assertThat(url).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_annual");
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_annual_test");
        assertThat(sub.getPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(sub.getBillingPeriod()).isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    void startCheckoutAnnualFallsBackToMonthlyPriceWhenAnnualNotConfigured() {
        properties.setPersonalAnnualPriceId("");
        User user = newUser("fallback@example.com");
        when(gateway.createSubscriptionSession(
                eq(null), any(), eq("price_personal_test"), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_fb", "cs_fb", "cus_fb"));

        String url = billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.ANNUAL);

        assertThat(url).isEqualTo("https://checkout.stripe.com/c/pay/cs_fb");
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_test");
        // Fallback drove the price down to the monthly SKU — the
        // recorded period has to reflect what Stripe will actually
        // charge, not what the user originally asked for.
        assertThat(sub.getBillingPeriod()).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void startCheckoutNullPeriodDefaultsToMonthly() {
        User user = newUser("nullperiod@example.com");
        when(gateway.createSubscriptionSession(
                eq(null), any(), eq("price_personal_test"), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_np", "cs_np", "cus_np"));

        String url = billingService.startCheckout(user, Plan.PERSONAL, null);

        assertThat(url).isEqualTo("https://checkout.stripe.com/c/pay/cs_np");
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_test");
    }

    @Test
    void secondCheckoutReusesExistingStripeCustomer() {
        User user = newUser("bob@example.com");
        when(gateway.createSubscriptionSession(
                eq(null), any(), eq("price_personal_test"), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/first",
                        "cs_first", "cus_bob"));
        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        ArgumentCaptor<String> existingCustomer = ArgumentCaptor.forClass(String.class);
        when(gateway.createSubscriptionSession(
                existingCustomer.capture(), any(), eq("price_team_test"), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/second",
                        "cs_second", "cus_bob"));

        String url = billingService.startCheckout(user, Plan.TEAM, BillingPeriod.MONTHLY);

        assertThat(url).isEqualTo("https://checkout.stripe.com/c/pay/second");
        assertThat(existingCustomer.getValue()).isEqualTo("cus_bob");
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getPlan()).isEqualTo(Plan.TEAM);
        assertThat(sub.getStripePriceId()).isEqualTo("price_team_test");
        assertThat(subscriptions.count()).isEqualTo(1L);
    }

    @Test
    void enterprisePlanIsRejectedForSelfServeCheckout() {
        User user = newUser("ent@example.com");
        assertThatThrownBy(() -> billingService.startCheckout(user, Plan.ENTERPRISE, BillingPeriod.MONTHLY))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("sales");
    }

    @Test
    void freePlanIsRejectedForCheckout() {
        User user = newUser("free@example.com");
        assertThatThrownBy(() -> billingService.startCheckout(user, Plan.FREE, BillingPeriod.MONTHLY))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("Free");
    }

    @Test
    void missingPriceIdRaisesBillingException() {
        properties.setPersonalPriceId("");
        properties.setPersonalAnnualPriceId("");
        User user = newUser("noprice@example.com");
        assertThatThrownBy(() -> billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("price");
    }

    @Test
    void startPortalReturnsGatewayUrlForUserWithStripeCustomer() {
        User user = newUser("portal@example.com");
        when(gateway.createSubscriptionSession(eq(null), any(), any(), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_p", "cs_p", "cus_portal"));
        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        when(portalGateway.createPortalSession(eq("cus_portal"),
                eq("http://localhost:8080/threads")))
                .thenReturn("https://billing.stripe.com/p/session/abc");

        assertThat(billingService.startPortal(user))
                .contains("https://billing.stripe.com/p/session/abc");
    }

    @Test
    void startPortalReturnsEmptyWhenUserHasNoSubscription() {
        User user = newUser("nosub@example.com");
        assertThat(billingService.startPortal(user)).isEmpty();
    }

    @Test
    void firstCheckoutLogsFreeToTargetPlanTransition() {
        User user = newUser("first@example.com");
        when(gateway.createSubscriptionSession(eq(null), any(), eq("price_personal_test"),
                any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_pt", "cs_pt", "cus_pt"));

        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        java.util.List<PlanChangeEvent> events = planChanges.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFromPlan()).isEqualTo(Plan.FREE);
        assertThat(events.get(0).getToPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(events.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void upgradingPersonalToTeamLogsPersonalToTeamTransition() {
        User user = newUser("upgrader@example.com");
        when(gateway.createSubscriptionSession(eq(null), any(), eq("price_personal_test"),
                any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_p1", "cs_p1", "cus_up"));
        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        when(gateway.createSubscriptionSession(eq("cus_up"), any(), eq("price_team_test"),
                any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_t1", "cs_t1", "cus_up"));
        billingService.startCheckout(user, Plan.TEAM, BillingPeriod.MONTHLY);

        java.util.List<PlanChangeEvent> events = planChanges.findAll();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFromPlan()).isEqualTo(Plan.FREE);
        assertThat(events.get(0).getToPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(events.get(1).getFromPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(events.get(1).getToPlan()).isEqualTo(Plan.TEAM);
    }

    @Test
    void restartingCheckoutOnSamePlanDoesNotLogADuplicateTransition() {
        User user = newUser("retry@example.com");
        when(gateway.createSubscriptionSession(any(), any(), eq("price_personal_test"),
                any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_a", "cs_a", "cus_a"));
        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);
        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        assertThat(planChanges.findAll()).hasSize(1);
    }

    @Test
    void hasManagedBillingTrueOnlyAfterCheckoutAttachesCustomerId() {
        User user = newUser("hasbilling@example.com");
        assertThat(billingService.hasManagedBilling(user)).isFalse();

        when(gateway.createSubscriptionSession(eq(null), any(), any(), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/cs_q", "cs_q", "cus_q"));
        billingService.startCheckout(user, Plan.PERSONAL, BillingPeriod.MONTHLY);

        assertThat(billingService.hasManagedBilling(user)).isTrue();
    }
}
