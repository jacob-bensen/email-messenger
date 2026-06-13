package com.emailmessenger.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Component
class StripeSubscriptionGatewayImpl implements StripeSubscriptionGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeSubscriptionGatewayImpl.class);

    private final BillingProperties properties;

    StripeSubscriptionGatewayImpl(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> currentPriceId(String stripeSubscriptionId) {
        if (!StringUtils.hasText(stripeSubscriptionId)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new BillingException(
                    "Stripe is not configured. Set billing.stripe.secret-key (STRIPE_SECRET_KEY).");
        }
        Stripe.apiKey = properties.getSecretKey();
        try {
            Subscription sub = Subscription.retrieve(stripeSubscriptionId);
            if (sub == null || sub.getItems() == null) {
                return Optional.empty();
            }
            List<SubscriptionItem> items = sub.getItems().getData();
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }
            Price price = items.get(0).getPrice();
            if (price == null || !StringUtils.hasText(price.getId())) {
                return Optional.empty();
            }
            return Optional.of(price.getId());
        } catch (StripeException e) {
            log.warn("Stripe subscription retrieve failed for {}: {}", stripeSubscriptionId, e.getMessage());
            return Optional.empty();
        }
    }
}
