package com.emailmessenger.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
class StripeWebhookGatewayImpl implements StripeWebhookGateway {

    private final BillingProperties properties;
    private final ObjectMapper objectMapper;

    StripeWebhookGatewayImpl(BillingProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public StripeEvent parse(String payload, String signatureHeader) {
        if (!StringUtils.hasText(properties.getWebhookSecret())) {
            throw new BillingException(
                    "Stripe webhook secret is not configured. Set billing.stripe.webhook-secret (STRIPE_WEBHOOK_SECRET).");
        }
        Event verified;
        try {
            verified = Webhook.constructEvent(payload, signatureHeader, properties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new InvalidStripeSignatureException("Stripe webhook signature did not verify", e);
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data").path("object");
            return new StripeEvent(
                    verified.getId(),
                    verified.getType(),
                    customerIdOf(data),
                    subscriptionIdOf(verified.getType(), data),
                    text(data, "status"),
                    priceIdOf(data),
                    instant(data, "trial_end"),
                    currentPeriodEndOf(data));
        } catch (Exception e) {
            throw new BillingException("Failed to parse Stripe webhook payload: " + e.getMessage(), e);
        }
    }

    private String customerIdOf(JsonNode data) {
        return text(data, "customer");
    }

    private String subscriptionIdOf(String type, JsonNode data) {
        // checkout.session.completed: subscription is a top-level field.
        // customer.subscription.*: the object itself is the subscription, so use its id.
        if ("checkout.session.completed".equals(type)) {
            return text(data, "subscription");
        }
        if (type != null && type.startsWith("customer.subscription.")) {
            return text(data, "id");
        }
        return null;
    }

    private String priceIdOf(JsonNode data) {
        // subscription.items.data[0].price.id
        JsonNode firstItem = data.path("items").path("data").path(0);
        String fromItems = text(firstItem.path("price"), "id");
        if (StringUtils.hasText(fromItems)) {
            return fromItems;
        }
        // checkout sessions don't include line items in the webhook payload.
        return null;
    }

    private Instant currentPeriodEndOf(JsonNode data) {
        // Stripe API ≥ 2024-09: per-item; older: top-level on the subscription.
        Instant top = instant(data, "current_period_end");
        if (top != null) {
            return top;
        }
        JsonNode firstItem = data.path("items").path("data").path(0);
        return instant(firstItem, "current_period_end");
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull() || !v.isTextual()) {
            return null;
        }
        String s = v.asText();
        return s.isEmpty() ? null : s;
    }

    private static Instant instant(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull() || !v.canConvertToLong() || v.asLong() <= 0) {
            return null;
        }
        return Instant.ofEpochSecond(v.asLong());
    }
}
