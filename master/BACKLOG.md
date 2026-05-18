# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

1. **Trial-status banner inside the inbox** — read `Subscription.status`
   + `trialEndsAt` and render a "Trial ends in N days — add card" banner
   plus a `subscriptionEndedBanner` lockout when status is `canceled`.
   (Milestone 3)

2. **Stripe customer portal integration** — `/billing/portal` redirect to
   a Stripe-hosted billing portal session so users can self-serve
   upgrade/downgrade/cancel; reduces churn from "I can't cancel". (Milestone 3)

3. **Plan-limit enforcement** — service-layer guard that rejects a 2nd
   mailbox or 501st thread on Free; surfaces a typed
   `PlanLimitExceededException` for the upgrade modal to catch. (Milestone 4)

4. **Upgrade modal at free-tier limit** — when `PlanLimitExceededException`
   is thrown, render a plan-comparison modal over the thread list with a
   one-click "Upgrade to Personal" CTA into Checkout. (Milestone 4)
