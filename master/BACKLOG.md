# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

- **Cancellation reason capture at the point of cancel.**
  Add a one-question reason picker on the Stripe-Portal-return path
  with persisted enum on the Subscription row.
  → EPIC-17 Milestone 2.

- **At-risk retention queue on `/admin/revenue`.**
  Card or page listing the last N `active → canceled` rows in the
  window with plan, cadence, source, and reason — the win-back list.
  → EPIC-17 Milestone 3.

- **Per-plan churn line in the operator weekly digest.**
  Extend `AdminWeeklyDigestService` so the Monday email reads
  "Personal: N canceled (-$X MRR); Team: …" alongside the existing
  MRR / new-paying / churn totals.
  → EPIC-17 Milestone 4.
