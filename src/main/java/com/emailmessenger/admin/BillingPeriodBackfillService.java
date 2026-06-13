package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.BillingProperties;
import com.emailmessenger.billing.StripeSubscriptionGateway;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Backfills {@code subscriptions.billing_period} for pre-V17 rows by asking
 * Stripe for the live price ID and reverse-mapping it through
 * {@link BillingProperties#periodFor(String)}. Idempotent: a row only stays
 * in the candidate set while its {@code billing_period} is NULL, so a
 * second run after the first finishes is a no-op.
 */
@Service
public class BillingPeriodBackfillService {

    private static final Logger log = LoggerFactory.getLogger(BillingPeriodBackfillService.class);

    private final SubscriptionRepository subscriptions;
    private final StripeSubscriptionGateway gateway;
    private final BillingProperties properties;

    BillingPeriodBackfillService(SubscriptionRepository subscriptions,
                                 StripeSubscriptionGateway gateway,
                                 BillingProperties properties) {
        this.subscriptions = subscriptions;
        this.gateway = gateway;
        this.properties = properties;
    }

    @Transactional
    public BillingPeriodBackfillResult reconcile() {
        List<Subscription> candidates = subscriptions.findByBillingPeriodIsNull();
        if (candidates.isEmpty()) {
            return BillingPeriodBackfillResult.empty();
        }

        int updated = 0;
        int missingStripeId = 0;
        int unmatchedPriceId = 0;
        int stripeMisses = 0;

        for (Subscription sub : candidates) {
            String stripeSubId = sub.getStripeSubscriptionId();
            if (!StringUtils.hasText(stripeSubId)) {
                missingStripeId++;
                continue;
            }
            Optional<String> priceId = gateway.currentPriceId(stripeSubId);
            if (priceId.isEmpty()) {
                stripeMisses++;
                continue;
            }
            BillingPeriod period = properties.periodFor(priceId.get());
            if (period == null) {
                log.info("Reconcile: price {} on sub {} doesn't match a configured SKU; leaving billing_period null",
                        priceId.get(), stripeSubId);
                unmatchedPriceId++;
                continue;
            }
            sub.setStripePriceId(priceId.get());
            sub.setBillingPeriod(period);
            updated++;
        }

        log.info("Reconcile complete: scanned={} updated={} missingStripeId={} unmatched={} stripeMisses={}",
                candidates.size(), updated, missingStripeId, unmatchedPriceId, stripeMisses);
        return new BillingPeriodBackfillResult(
                candidates.size(), updated, missingStripeId, unmatchedPriceId, stripeMisses);
    }
}
