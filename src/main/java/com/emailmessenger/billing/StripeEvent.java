package com.emailmessenger.billing;

import java.time.Instant;

/**
 * Slice of a Stripe webhook event flattened to just the fields the
 * billing service needs to mirror subscription state locally. The
 * {@link StripeWebhookGateway} verifies the signature and parses the
 * raw payload into this record; everything downstream is SDK-agnostic
 * and trivially mockable in tests.
 */
public record StripeEvent(
        String id,
        String type,
        String customerId,
        String subscriptionId,
        String status,
        String priceId,
        Instant trialEnd,
        Instant currentPeriodEnd) {
}
