-- EPIC-17 Milestone 2: cancellation reason captured at the point of cancel.
--
-- The /admin/revenue churn card already counts how many subs walked out
-- and how much MRR went with them, but the operator can't tell *why*
-- they walked. The in-app pre-cancel step on /billing/cancel-subscription
-- now asks one question (too expensive / missing feature / switching /
-- temporary / other) before bouncing the user to the Stripe Billing
-- Portal to finalize. The captured enum lands here and feeds the
-- at-risk retention queue (M3) and the operator weekly digest (M4).
--
-- Nullable: a user who cancels directly in the Stripe Portal without
-- going through the in-app picker doesn't get a forced default — the
-- field stays null and the operator reads it as "unrecorded" instead
-- of a misleading "other". `cancellation_reason_at` stamps when the
-- in-app picker recorded the reason (which is typically minutes before
-- the Stripe webhook flips status to canceled).
ALTER TABLE subscriptions ADD COLUMN cancellation_reason    VARCHAR(32);
ALTER TABLE subscriptions ADD COLUMN cancellation_reason_at TIMESTAMP;
