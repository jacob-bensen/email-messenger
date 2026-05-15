package com.emailmessenger.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class StripeCheckoutGatewayImpl implements StripeCheckoutGateway {

    private final BillingProperties properties;

    StripeCheckoutGatewayImpl(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public CheckoutSessionResult createSubscriptionSession(
            String existingCustomerId,
            String customerEmail,
            String priceId,
            Integer trialPeriodDays,
            String successUrl,
            String cancelUrl) {

        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new BillingException(
                    "Stripe is not configured. Set billing.stripe.secret-key (STRIPE_SECRET_KEY).");
        }
        if (!StringUtils.hasText(priceId)) {
            throw new BillingException("No Stripe price ID configured for the requested plan.");
        }
        Stripe.apiKey = properties.getSecretKey();

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .setAllowPromotionCodes(true);

        if (StringUtils.hasText(existingCustomerId)) {
            params.setCustomer(existingCustomerId);
        } else {
            params.setCustomerEmail(customerEmail);
            params.setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS);
        }

        if (trialPeriodDays != null && trialPeriodDays > 0) {
            params.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .setTrialPeriodDays(trialPeriodDays.longValue())
                    .build());
        }

        try {
            Session session = Session.create(params.build());
            return new CheckoutSessionResult(session.getUrl(), session.getId(), session.getCustomer());
        } catch (StripeException e) {
            throw new BillingException("Stripe Checkout Session creation failed: " + e.getMessage(), e);
        }
    }
}
