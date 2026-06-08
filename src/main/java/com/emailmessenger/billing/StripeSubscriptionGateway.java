package com.emailmessenger.billing;

import java.util.Optional;

public interface StripeSubscriptionGateway {

    /**
     * Returns the active price ID currently attached to the Stripe
     * subscription with the given id, or {@link Optional#empty()} when the
     * subscription cannot be retrieved (deleted / unknown / no items / SDK
     * error). The reverse-mapping to a {@link BillingPeriod} is the
     * caller's responsibility via {@link BillingProperties#periodFor}.
     */
    Optional<String> currentPriceId(String stripeSubscriptionId);
}
