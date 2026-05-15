package com.emailmessenger.billing;

public interface StripeCheckoutGateway {

    /**
     * Creates a Stripe Checkout Session in subscription mode. If
     * {@code existingCustomerId} is non-null, the session is bound to that
     * customer; otherwise Stripe creates a new customer keyed by the email.
     * A non-null {@code trialPeriodDays} attaches a trial to the resulting
     * subscription so checkout can complete without an immediate charge.
     */
    CheckoutSessionResult createSubscriptionSession(
            String existingCustomerId,
            String customerEmail,
            String priceId,
            Integer trialPeriodDays,
            String successUrl,
            String cancelUrl);
}
