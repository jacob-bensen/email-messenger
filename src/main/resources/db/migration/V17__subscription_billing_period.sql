-- EPIC-11 Milestone 4: record the billing cadence next to each Subscription.
--
-- We already capture stripe_price_id, but answering "are you on monthly or
-- annual?" without a Stripe API round-trip means we need the period denormalised
-- locally — both for the /account self-serve display ("Personal · Annual,
-- renews YYYY-MM-DD") and for a future revenue-mix readout. Nullable because:
--   * pre-EPIC-11 rows have no recorded period; we backfill MONTHLY for any
--     row whose status is not 'incomplete' (a paid or trialing customer can
--     only have come through the monthly checkout before this migration);
--   * incomplete rows have no Stripe state yet — the period is populated when
--     startCheckout selects a SKU or the first webhook lands.
ALTER TABLE subscriptions ADD COLUMN billing_period VARCHAR(10);

UPDATE subscriptions SET billing_period = 'MONTHLY'
 WHERE status <> 'incomplete' AND billing_period IS NULL;
