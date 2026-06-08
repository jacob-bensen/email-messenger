# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

- [ ] **Annual switch from in-app upgrade modal + trial nudge**
      `/threads` upgrade modal gains Monthly|Annual sub-toggle on its
      Stripe Checkout form; trial-conversion nudge surfaces an annual
      CTA in its final-3-days copy so trial-end converts at higher ARPU.
      Milestone 3.

- [ ] **Subscription billing_period field + active-cadence display**
      Flyway V17 adds `subscriptions.billing_period VARCHAR(10)`;
      `applyStripeEvent` derives it from the matched price ID; `/account`
      shows "Personal · Annual, renews YYYY-MM-DD" so a paying customer
      can verify their cadence without contacting support.
      Milestone 4.
