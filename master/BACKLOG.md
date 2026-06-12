# BACKLOG

Up to 10 items, each advancing a PLAN.md milestone. Pick from the top.

1. **User registration & authentication** — Spring Security email/password
   with BCrypt, login/logout/registration controllers, `User` entity, and
   session management. (Milestone 1)

2. **Make threads user-owned** — add `user_id` FK to `EmailThread` (Flyway
   migration), filter all repository queries by current principal, and
   migrate existing single-tenant code paths. (Milestone 1)

3. **CSRF protection for POST endpoints** — enable Spring Security CSRF,
   add hidden token to `conversation.html` reply form and any future
   forms; required the moment auth ships or the reply POST returns 403.
   (Milestone 1)

4. **Stripe billing integration** — Stripe Java SDK, `BillingService`,
   `/billing/checkout` that creates a Checkout Session per plan, plus a
   local `Subscription` entity persisted from webhook events. (Milestone 2)

5. **Stripe webhook handler** — `POST /billing/webhook` validating the
   signature, handling `checkout.session.completed`,
   `customer.subscription.updated`, `customer.subscription.deleted`.
   (Milestone 2)

6. **Wire pricing-page CTAs into the funnel** — pricing CTAs currently
   dead-end at `/threads`; point them at `/signup?plan=personal|team` and
   chain straight into Stripe Checkout after registration. (Milestone 2)

7. **14-day free trial on Personal tier** — set `trial_period_days=14` on
   the Personal price, allow checkout without a card up-front, render
   "Trial ends DATE" banner inside the inbox. (Milestone 3)

8. **Stripe customer portal integration** — `/billing/portal` redirect to
   a Stripe-hosted billing portal session so users can self-serve
   upgrade/downgrade/cancel; reduces churn from "I can't cancel". (Milestone 3)

9. **Plan-limit enforcement** — service-layer guard that rejects a 2nd
   mailbox or 501st thread on Free; surfaces a typed
   `PlanLimitExceededException` for the upgrade modal to catch. (Milestone 4)

10. **Upgrade modal at free-tier limit** — when `PlanLimitExceededException`
    is thrown, render a plan-comparison modal over the thread list with a
    one-click "Upgrade to Personal" CTA into Checkout. (Milestone 4)
