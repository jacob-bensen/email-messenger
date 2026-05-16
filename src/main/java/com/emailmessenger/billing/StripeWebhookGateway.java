package com.emailmessenger.billing;

public interface StripeWebhookGateway {

    /**
     * Verifies the Stripe-Signature header against the raw payload and
     * extracts the fields the billing service cares about. Throws
     * {@link InvalidStripeSignatureException} on bad signature so the
     * controller can return 400, and {@link BillingException} if the
     * webhook secret is not configured.
     */
    StripeEvent parse(String payload, String signatureHeader);
}
