# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

## Funnel conversion rates (signup → trial → paid, 30-day rolling)
Add three conversion-rate panes to `/admin/revenue` broken down by
`users.acquisition_source` so channel quality is visible per source.
Advances: EPIC-12 Milestone 3.

## Weekly operator digest email
Mon 09:00 UTC `@Scheduled` (gated by `admin.weekly-digest.enabled`) that
mails MRR / ARR / new-paying-this-week / churn to every `admin.emails`
recipient through the existing `JavaMailSender`.
Advances: EPIC-12 Milestone 4.
