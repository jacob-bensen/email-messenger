# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

- [ ] **Subscription billing_period field + active-cadence display**
      Flyway V17 adds `subscriptions.billing_period VARCHAR(10)`;
      `applyStripeEvent` derives it from the matched price ID; `/account`
      shows "Personal · Annual, renews YYYY-MM-DD" so a paying customer
      can verify their cadence without contacting support.
      Milestone 4.
