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
count, history, and saved-search counts grow. EPICs 02–11 (Monetization,
Mailbox Onboarding, Deployability, Acquisition, Launch readiness, Inbox
Search, Saved Searches & Reactivation, Account self-serve, Mobile/PWA,
Annual billing surfacing) are code-complete in `claude_routine`; live
deploy is gated on Master ops (hosting, domain, Stripe live keys,
encryption secrets, demo video URL).

## Primary Objective

**Ship EPIC-12 First-paying-customer attribution funnel.** The product
records who signed up (`users.acquisition_source` since V8), who
converted to paid (`subscriptions` rows with `plan` + `billing_period`
since V17), and when (`updated_at` timestamps everywhere) — but nothing
in the UI surfaces any of this. An operator who's just deployed MailIM
has no way to answer "is this thing making money?", "where are my paying
customers coming from?", or "what's my monthly vs. annual ARPU mix?"
without opening the Stripe dashboard, reading raw SQL, or both. That gap
is what turns a launched product into one that grows: once the operator
can see which acquisition channels actually convert to paid (vs. just
to trial), they can double down on the working channels and kill the
losers — the single most leveraged revenue decision available to a
solo-operator SaaS. This Objective ends when the operator can answer
those three questions from a single `/admin/revenue` page (visible only
to the allowlisted operator emails), pre-V17 subscriptions have their
`billing_period` backfilled from Stripe so the annual-mix metric isn't
silently wrong, signup-to-trial and trial-to-paid funnel conversion
rates are surfaced, and a weekly email digest delivers the same metrics
so the operator doesn't have to remember to check.

## Milestones

1. **`/admin/revenue` operator dashboard.** New `admin.emails` config
   (comma-separated allowlist) gates `/admin/**` — non-admins get 404 so
   the surface stays invisible to non-operators. The page renders six KPI
   tiles (MRR, ARR, active subscribers, in-trial count + trial pipeline,
   annual mix %, trials ending in 7 days) plus three tables: per-plan
   monthly/annual/MRR breakdown, acquisition-source breakdown sorted by
   MRR contribution (using existing `users.acquisition_source`), and the
   last 10 subscription events with email/plan/cadence/status/when.
2. **Stripe backfill for pre-V17 `billing_period`.** A one-shot job that
   iterates `subscriptions` rows where `billing_period IS NULL`, fetches
   the live subscription via the Stripe API, matches the active price ID
   against `BillingProperties.periodFor(...)`, and writes the inferred
   `BillingPeriod` back. Triggered on demand from the admin dashboard
   ("Reconcile from Stripe" button) and idempotent so a second run is a
   no-op. Required so the annual-mix metric is accurate for accounts
   that signed up before V17 shipped.
3. **Funnel conversion rates (signup → trial → paid).** Extend the
   dashboard with three rolling conversion-rate panes: signups in last
   30d, trial starts in last 30d (% of signups), paid conversions in
   last 30d (% of trial starts). Break each down by
   `users.acquisition_source` so the operator can see channel-quality
   differences. Adds the data needed to make per-channel
   acquisition-spend decisions.
4. **Weekly operator email digest.** [shipped 2026-06-08] Mails MRR /
   ARR / active subscriber count / monthly-annual mix / trial pipeline +
   trials ending in 7 days / new paying customers in last 7 days /
   churn in last 7 days / `/admin/revenue` link to every
   `admin.emails` address. Cron `ADMIN_WEEKLY_DIGEST_CRON` (default
   `0 0 9 ? * MON` UTC), zone `ADMIN_WEEKLY_DIGEST_ZONE`, scheduler
   gated by `ADMIN_WEEKLY_DIGEST_ENABLED=true`.

## Done means

An operator visits `/admin/revenue` and sees live MRR, ARR, active
subscribers, monthly/annual mix percentage, trials ending in the next 7
days, per-plan revenue breakdown, and per-acquisition-source MRR
contribution — all from data already in the application database, with
no Stripe API round-trip on page load. Non-admin authenticated users
hitting the URL get a 404 (no surface leak). The pre-V17 backfill job
has completed so every active subscription has a `billing_period`
populated. The 30-day funnel pane shows signup → trial → paid
conversion rates, broken down by source. The operator receives a Monday
weekly summary email with the same headline metrics.
