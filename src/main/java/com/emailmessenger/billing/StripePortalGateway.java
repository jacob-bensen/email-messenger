package com.emailmessenger.billing;

public interface StripePortalGateway {

    /**
     * Creates a Stripe-hosted Billing Portal session for {@code customerId}
     * and returns the redirect URL. {@code returnUrl} is where Stripe sends
     * the user when they click "Return to MailIM".
     */
    String createPortalSession(String customerId, String returnUrl);
}
