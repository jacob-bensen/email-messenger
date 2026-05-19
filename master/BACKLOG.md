# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

1. **Plan-limit enforcement** — service-layer guard that rejects a 2nd
   mailbox or 501st thread on Free; surfaces a typed
   `PlanLimitExceededException` for the upgrade modal to catch. (Milestone 4)

2. **Upgrade modal at free-tier limit** — when `PlanLimitExceededException`
   is thrown, render a plan-comparison modal over the thread list with a
   one-click "Upgrade to Personal" CTA into Checkout. (Milestone 4)
