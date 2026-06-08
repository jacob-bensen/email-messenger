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
    private final StripePortalGateway portalGateway;
    private final BillingProperties properties;

    BillingService(SubscriptionRepository subscriptions,
                   StripeCheckoutGateway gateway,
                   StripePortalGateway portalGateway,
                   BillingProperties properties) {
        this.subscriptions = subscriptions;
        this.gateway = gateway;
        this.portalGateway = portalGateway;
        this.properties = properties;
    }

    @Transactional
    public String startCheckout(User user, Plan plan, BillingPeriod period) {
        if (plan == Plan.FREE) {
            throw new BillingException("Free plan does not require checkout.");
        }
        if (plan == Plan.ENTERPRISE) {
            throw new BillingException("Enterprise is sales-assisted; use the contact link.");
        }
        BillingPeriod resolved = period == null ? BillingPeriod.MONTHLY : period;
        String priceId = properties.priceIds(resolved).get(plan);
        if (!StringUtils.hasText(priceId) && resolved == BillingPeriod.ANNUAL) {
            // Annual SKU not yet configured for this plan — degrade to
            // monthly so a customer who picked "Annual" still completes
            // checkout. The Stripe Billing Portal will offer the annual
            // swap once the price ID is wired.
            priceId = properties.priceIds(BillingPeriod.MONTHLY).get(plan);
            resolved = BillingPeriod.MONTHLY;
        }
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
        sub.setBillingPeriod(resolved);
        if (existing == null) {
            subscriptions.save(sub);
        }
        return result.url();
    }

    /**
     * Returns a Stripe Billing Portal URL the user can be redirected to for
     * self-serve plan changes, payment-method updates, invoices and
     * cancellation. Empty when the user has not yet completed a Checkout
     * (no Stripe customer to manage) — caller should send them to /pricing.
     */
    @Transactional(readOnly = true)
    public Optional<String> startPortal(User user) {
        Subscription sub = subscriptions.findByUser(user).orElse(null);
        if (sub == null || !StringUtils.hasText(sub.getStripeCustomerId())) {
            return Optional.empty();
        }
        String url = portalGateway.createPortalSession(
                sub.getStripeCustomerId(), properties.getPortalReturnUrl());
        return Optional.of(url);
    }

    /**
     * True when the user has completed at least one Stripe Checkout (a
     * customer id exists) so a "Manage billing" UI affordance has somewhere
     * meaningful to point at.
     */
    @Transactional(readOnly = true)
    public boolean hasManagedBilling(User user) {
        return subscriptions.findByUser(user)
                .map(Subscription::getStripeCustomerId)
                .filter(StringUtils::hasText)
                .isPresent();
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
            BillingPeriod derived = properties.periodFor(event.priceId());
            if (derived != null) {
                sub.setBillingPeriod(derived);
            }
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
