package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final SubscriptionRepository subscriptions;
    private final StripeCheckoutGateway gateway;
    private final BillingProperties properties;

    BillingService(SubscriptionRepository subscriptions,
                   StripeCheckoutGateway gateway,
                   BillingProperties properties) {
        this.subscriptions = subscriptions;
        this.gateway = gateway;
        this.properties = properties;
    }

    @Transactional
    public String startCheckout(User user, Plan plan) {
        if (plan == Plan.ENTERPRISE) {
            throw new BillingException("Enterprise is sales-assisted; use the contact link.");
        }
        String priceId = properties.priceIds().get(plan);
        if (!StringUtils.hasText(priceId)) {
            throw new BillingException("No price configured for plan " + plan);
        }

        Subscription existing = subscriptions.findByUser(user).orElse(null);
        String existingCustomerId = existing != null ? existing.getStripeCustomerId() : null;

        CheckoutSessionResult result = gateway.createSubscriptionSession(
                existingCustomerId,
                user.getEmail(),
                priceId,
                properties.getTrialDays(),
                properties.getSuccessUrl(),
                properties.getCancelUrl());

        Subscription sub = existing != null
                ? existing
                : new Subscription(user, result.customerId(), "incomplete");
        sub.setPlan(plan);
        sub.setStripePriceId(priceId);
        if (existing == null) {
            subscriptions.save(sub);
        }
        return result.url();
    }

    /**
     * Mirrors a Stripe webhook event onto the local Subscription row.
     * Idempotent: replaying the same event flips the row to the same
     * terminal state. Unknown customers / unknown event types are
     * ignored so we always return 200 to Stripe and avoid retry storms.
     */
    @Transactional
    public void applyStripeEvent(StripeEvent event) {
        if (event == null || event.type() == null) {
            return;
        }
        switch (event.type()) {
            case "checkout.session.completed" -> applyCheckoutCompleted(event);
            case "customer.subscription.created", "customer.subscription.updated" -> applySubscriptionUpdated(event);
            case "customer.subscription.deleted" -> applySubscriptionDeleted(event);
            default -> log.debug("Ignoring Stripe event type {}", event.type());
        }
    }

    private void applyCheckoutCompleted(StripeEvent event) {
        if (event.customerId() == null) {
            log.warn("checkout.session.completed event {} missing customer; skipping", event.id());
            return;
        }
        Subscription sub = subscriptions.findByStripeCustomerId(event.customerId()).orElse(null);
        if (sub == null) {
            log.warn("checkout.session.completed for unknown customer {}; skipping", event.customerId());
            return;
        }
        if (event.subscriptionId() != null) {
            sub.setStripeSubscriptionId(event.subscriptionId());
        }
        // Checkout-completed alone doesn't carry the subscription status; the
        // follow-up customer.subscription.created event does. Until that
        // arrives, mark trialing if a trial is configured, else active.
        if ("incomplete".equals(sub.getStatus())) {
            sub.setStatus(properties.getTrialDays() > 0 ? "trialing" : "active");
        }
        if (event.trialEnd() != null) {
            sub.setTrialEndsAt(toLocal(event.trialEnd()));
        }
        if (event.currentPeriodEnd() != null) {
            sub.setCurrentPeriodEnd(toLocal(event.currentPeriodEnd()));
        }
    }

    private void applySubscriptionUpdated(StripeEvent event) {
        Subscription sub = locate(event);
        if (sub == null) {
            log.warn("{} for unknown subscription/customer (sub={} cust={}); skipping",
                    event.type(), event.subscriptionId(), event.customerId());
            return;
        }
        if (event.subscriptionId() != null && sub.getStripeSubscriptionId() == null) {
            sub.setStripeSubscriptionId(event.subscriptionId());
        }
        if (StringUtils.hasText(event.status())) {
            sub.setStatus(event.status());
        }
        if (event.priceId() != null) {
            sub.setStripePriceId(event.priceId());
        }
        if (event.trialEnd() != null) {
            sub.setTrialEndsAt(toLocal(event.trialEnd()));
        }
        if (event.currentPeriodEnd() != null) {
            sub.setCurrentPeriodEnd(toLocal(event.currentPeriodEnd()));
        }
    }

    private void applySubscriptionDeleted(StripeEvent event) {
        Subscription sub = locate(event);
        if (sub == null) {
            log.warn("customer.subscription.deleted for unknown subscription/customer (sub={} cust={}); skipping",
                    event.subscriptionId(), event.customerId());
            return;
        }
        sub.setStatus("canceled");
    }

    private Subscription locate(StripeEvent event) {
        if (StringUtils.hasText(event.subscriptionId())) {
            Optional<Subscription> bySub = subscriptions.findByStripeSubscriptionId(event.subscriptionId());
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }
        if (StringUtils.hasText(event.customerId())) {
            return subscriptions.findByStripeCustomerId(event.customerId()).orElse(null);
        }
        return null;
    }

    private static LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
