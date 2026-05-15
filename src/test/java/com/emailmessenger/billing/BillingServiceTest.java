package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
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

    @MockBean StripeCheckoutGateway gateway;
    @MockBean ReplyService replyService;

    @BeforeEach
    void seedPriceIds() {
        properties.setPersonalPriceId("price_personal_test");
        properties.setTeamPriceId("price_team_test");
        properties.setSuccessUrl("http://localhost:8080/billing/success");
        properties.setCancelUrl("http://localhost:8080/billing/cancel");
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

        String url = billingService.startCheckout(user, Plan.PERSONAL);

        assertThat(url).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_abc");
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripeCustomerId()).isEqualTo("cus_test_123");
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_test");
        assertThat(sub.getPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(sub.getStatus()).isEqualTo("incomplete");
        assertThat(sub.getStripeSubscriptionId()).isNull();
    }

    @Test
    void secondCheckoutReusesExistingStripeCustomer() {
        User user = newUser("bob@example.com");
        when(gateway.createSubscriptionSession(
                eq(null), any(), eq("price_personal_test"), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/first",
                        "cs_first", "cus_bob"));
        billingService.startCheckout(user, Plan.PERSONAL);

        ArgumentCaptor<String> existingCustomer = ArgumentCaptor.forClass(String.class);
        when(gateway.createSubscriptionSession(
                existingCustomer.capture(), any(), eq("price_team_test"), any(), any(), any()))
                .thenReturn(new CheckoutSessionResult(
                        "https://checkout.stripe.com/c/pay/second",
                        "cs_second", "cus_bob"));

        String url = billingService.startCheckout(user, Plan.TEAM);

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
        assertThatThrownBy(() -> billingService.startCheckout(user, Plan.ENTERPRISE))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("sales");
    }

    @Test
    void missingPriceIdRaisesBillingException() {
        properties.setPersonalPriceId("");
        User user = newUser("noprice@example.com");
        assertThatThrownBy(() -> billingService.startCheckout(user, Plan.PERSONAL))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("price");
    }
}
