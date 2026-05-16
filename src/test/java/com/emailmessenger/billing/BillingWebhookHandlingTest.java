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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BillingWebhookHandlingTest {

    @Autowired BillingService billingService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean StripeCheckoutGateway checkoutGateway;
    @MockBean ReplyService replyService;

    private User user;
    private Subscription pending;

    @BeforeEach
    void seedIncompleteSubscription() {
        userService.register("payer@example.com", "password1", "Payer");
        user = users.findByEmail("payer@example.com").orElseThrow();
        pending = new Subscription(user, "cus_test_payer", "incomplete");
        pending.setPlan(Plan.PERSONAL);
        pending.setStripePriceId("price_personal_test");
        subscriptions.save(pending);
    }

    @Test
    void checkoutCompletedAttachesSubscriptionIdAndFlipsToTrialing() {
        StripeEvent event = new StripeEvent(
                "evt_1", "checkout.session.completed",
                "cus_test_payer", "sub_test_xyz",
                null, null, null, null);

        billingService.applyStripeEvent(event);

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripeSubscriptionId()).isEqualTo("sub_test_xyz");
        assertThat(sub.getStatus()).isEqualTo("trialing");
    }

    @Test
    void subscriptionUpdatedSyncsStatusAndPeriodEnd() {
        pending.setStripeSubscriptionId("sub_test_abc");
        pending.setStatus("trialing");
        subscriptions.save(pending);

        Instant trialEnd = Instant.parse("2026-05-30T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-06-30T00:00:00Z");
        StripeEvent event = new StripeEvent(
                "evt_2", "customer.subscription.updated",
                "cus_test_payer", "sub_test_abc",
                "active", "price_personal_test", trialEnd, periodEnd);

        billingService.applyStripeEvent(event);

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo("active");
        assertThat(sub.getTrialEndsAt()).isEqualTo(LocalDateTime.ofInstant(trialEnd, ZoneOffset.UTC));
        assertThat(sub.getCurrentPeriodEnd()).isEqualTo(LocalDateTime.ofInstant(periodEnd, ZoneOffset.UTC));
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_test");
    }

    @Test
    void subscriptionUpdatedLocatesRowByCustomerWhenSubscriptionIdUnknown() {
        StripeEvent event = new StripeEvent(
                "evt_3", "customer.subscription.created",
                "cus_test_payer", "sub_test_new",
                "trialing", "price_personal_test", null, null);

        billingService.applyStripeEvent(event);

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripeSubscriptionId()).isEqualTo("sub_test_new");
        assertThat(sub.getStatus()).isEqualTo("trialing");
    }

    @Test
    void subscriptionDeletedFlipsToCanceled() {
        pending.setStripeSubscriptionId("sub_test_kill");
        pending.setStatus("active");
        subscriptions.save(pending);

        StripeEvent event = new StripeEvent(
                "evt_4", "customer.subscription.deleted",
                "cus_test_payer", "sub_test_kill",
                "canceled", null, null, null);

        billingService.applyStripeEvent(event);

        assertThat(subscriptions.findByUser(user).orElseThrow().getStatus()).isEqualTo("canceled");
    }

    @Test
    void unknownCustomerIsIgnoredSoStripeStopsRetrying() {
        StripeEvent event = new StripeEvent(
                "evt_5", "checkout.session.completed",
                "cus_unknown", "sub_unknown",
                null, null, null, null);

        billingService.applyStripeEvent(event);

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo("incomplete");
        assertThat(sub.getStripeSubscriptionId()).isNull();
    }

    @Test
    void replayingSameEventIsIdempotent() {
        StripeEvent first = new StripeEvent(
                "evt_6", "customer.subscription.updated",
                "cus_test_payer", "sub_test_replay",
                "active", "price_personal_test", null, null);

        billingService.applyStripeEvent(first);
        billingService.applyStripeEvent(first);

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo("active");
        assertThat(sub.getStripeSubscriptionId()).isEqualTo("sub_test_replay");
        assertThat(subscriptions.count()).isEqualTo(1L);
    }

    @Test
    void unknownEventTypeIsIgnored() {
        StripeEvent event = new StripeEvent(
                "evt_7", "invoice.payment_succeeded",
                "cus_test_payer", null, "paid", null, null, null);

        billingService.applyStripeEvent(event);

        // Nothing changed.
        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo("incomplete");
    }
}
