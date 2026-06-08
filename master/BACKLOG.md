# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

## Stripe backfill for pre-V17 billing_period
Iterate `subscriptions` rows where `billing_period IS NULL`, query Stripe
for the live price ID, persist the inferred period — so annual mix isn't
silently wrong for accounts that signed up before V17.
Advances: EPIC-12 Milestone 2.

## Funnel conversion rates (signup → trial → paid, 30-day rolling)
Add three conversion-rate panes to `/admin/revenue` broken down by
`users.acquisition_source` so channel quality is visible per source.
Advances: EPIC-12 Milestone 3.

## Weekly operator digest email
Mon 09:00 UTC `@Scheduled` (gated by `admin.weekly-digest.enabled`) that
mails MRR / ARR / new-paying-this-week / churn to every `admin.emails`
recipient through the existing `JavaMailSender`.
Advances: EPIC-12 Milestone 4.
