package com.emailmessenger.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class StripePortalGatewayImpl implements StripePortalGateway {

    private final BillingProperties properties;

    StripePortalGatewayImpl(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public String createPortalSession(String customerId, String returnUrl) {
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new BillingException(
                    "Stripe is not configured. Set billing.stripe.secret-key (STRIPE_SECRET_KEY).");
        }
        if (!StringUtils.hasText(customerId)) {
            throw new BillingException("Cannot open billing portal without a Stripe customer id.");
        }
        if (!StringUtils.hasText(returnUrl)) {
            throw new BillingException(
                    "Stripe portal return URL is not configured. Set billing.stripe.portal-return-url.");
        }
        Stripe.apiKey = properties.getSecretKey();

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(returnUrl)
                .build();
        try {
            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new BillingException("Stripe Billing Portal session creation failed: " + e.getMessage(), e);
        }
    }
}
