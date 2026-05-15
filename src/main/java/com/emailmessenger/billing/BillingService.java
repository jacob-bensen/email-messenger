package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BillingService {

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
}
