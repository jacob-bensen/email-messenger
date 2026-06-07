# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 500 threads, 1 saved search), Personal $9/mo
(3 mailboxes, unlimited history, unlimited saved searches),
Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo (SSO, audit).
Annual billing offers 2 months free. Money comes from recurring
subscriptions, with natural Free → Personal → Team upgrades as mailbox
count, history, and saved-search counts grow. EPICs 02–10 (Monetization,
Mailbox Onboarding, Deployability, Acquisition, Launch readiness, Inbox
Search, Saved Searches & Reactivation, Account self-serve, Mobile/PWA)
are code-complete in `claude_routine`; live deploy is gated on Master ops
(hosting, domain, Stripe live keys, encryption secrets, demo video URL).

## Primary Objective

**Ship EPIC-11 Annual billing surfacing.** The `/pricing` page already
ships a `Monthly | Annual` toggle that visually swaps the displayed price
($9 → $7, $29 → $24, $99 → $83) but the CTAs route through the monthly
Stripe price ID regardless — annual is cosmetic. Annual billing
materially lifts ARPU (12 × monthly vs. ~10 × monthly = ~17% per active
subscription), is the single highest-impact pricing change available
without new product features, and is the cheapest path to defensible
revenue (it reduces churn — an annual subscriber doesn't cancel inside
the term). Two months free is also a stronger anchor for the Free →
Personal upgrade decision than a discount badge. This Objective ends
when a visitor on `/pricing` can toggle Annual, click "Start 14-day free
trial", land in Stripe Checkout against the annual price ID, complete
checkout, and have the local `Subscription` row record the annual
`stripe_price_id` — and existing monthly subscribers can self-serve a
switch to annual from the billing portal or an in-app upsell, with the
trial nudge surfacing the annual option in the final week.

## Milestones

1. **Annual SKU plumbed end-to-end + pricing toggle wired to CTA.**
   `BillingPeriod` enum (MONTHLY / ANNUAL) with a lenient `parse(String)`
   that defaults to MONTHLY for blank / unknown / tampered input.
   `BillingProperties` grows `personal-annual-price-id` /
   `team-annual-price-id` / `enterprise-annual-price-id` env-overridable
   fields and `priceIds(BillingPeriod)` resolves the right map.
   `BillingService.startCheckout(User, Plan, BillingPeriod)` selects the
   period-correct price ID and degrades ANNUAL→MONTHLY when the annual
   SKU isn't configured (so checkout always completes). `/billing/checkout`,
   `/register`, `/login` all accept a `billing` request param and round
   it through to the Stripe Checkout session. `/pricing`'s
   Monthly|Annual toggle rewrites the four plan-card CTA hrefs to add
   `?billing=annual` when the annual tab is active, so the period the
   user picked on /pricing actually reaches Stripe.
2. **Annual savings copy + value framing on `/pricing` and `/register`.**
   The "Save 16%" badge becomes "2 months free" so the value frame
   matches how SaaS pricing comparison sites describe annual discounts;
   each plan card grows a "Billed annually as $X" sub-line under the
   monthly-equivalent price when the annual tab is active, so the user
   sees both the monthly mental model ($7/mo) and the cash they'll
   actually be charged today ($84) before clicking through. The
   `/register?plan=personal&billing=annual` flow renders an
   annual-billing badge in the auth card so a user who picked Annual on
   `/pricing` sees the choice acknowledged on the signup screen and
   doesn't lose context across the page transition. The trial-nudge
   modal's CTA copy switches to "Continue on Personal (annual — 2 months
   free)" when annual is the active period from the original signup.
3. **Annual switch from in-app upgrade + trial nudge + billing portal.**
   The `upgrade-modal` on `/threads` gains a Monthly|Annual sub-toggle
   above the "Upgrade to Personal" button, hidden-fielding `billing` on
   the `/billing/checkout` form. The trial-conversion nudge modal
   surfaces the annual option in its final-3-days copy ("Save 2 months
   by switching to annual today") with a dedicated CTA that posts to
   `/billing/checkout` with `billing=annual` so a user in the trial-end
   funnel converts at the higher ARPU SKU. Existing monthly subscribers
   can swap to annual from the Stripe Billing Portal (default Stripe
   portal behaviour, but verify the price-swap toggle is enabled).
4. **Subscription period field + admin-visible ARPU mix.** Add
   `subscriptions.billing_period VARCHAR(10)` (Flyway V17) populated
   from `applyStripeEvent` when `event.priceId()` matches the annual
   map, so the system can answer "what fraction of active subscribers
   are on annual" without a Stripe API round-trip. `/account` shows the
   active billing period ("Personal · Annual, renews 2027-06-07") so a
   self-serve user knows what they're paying for. Optional follow-on:
   a tiny `/admin/revenue` row that shows monthly / annual subscriber
   counts side by side for the operator to track the ARPU lift.

## Done means

A visitor on `/pricing` can toggle Annual, click "Start 14-day free
trial" on the Personal card, land on `/register?plan=personal&billing=annual`,
submit the form, and be redirected into a Stripe Checkout session whose
line item is the **annual** Personal price ID. The completed checkout
returns a `Subscription` row whose `stripe_price_id` is the annual SKU
and `billing_period = 'ANNUAL'`. Existing monthly subscribers can
self-serve switch to annual from either the `/threads` upgrade modal,
the trial nudge in the final 3 days, or the Stripe Billing Portal.
`/account` shows the active billing cadence so a customer can confirm
their pricing without contacting support.
