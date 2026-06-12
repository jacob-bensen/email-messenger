# PLAN

## What this is

**email-messenger** (product name: MailIM) is a Spring Boot web app that
imports email threads over IMAP and renders them as a modern IM-style chat
view — bubbles, avatars, day separators, dark mode — instead of the
nested-quote wall most mail clients show. It is positioned as a freemium
SaaS: Free (1 mailbox, 30-day history), Personal $9/mo (3 mailboxes,
unlimited history), Team $29/mo (10 mailboxes, sharing), Enterprise $99/mo
(SSO, audit). Annual billing offers 2 months free. Money comes from
recurring subscriptions, with natural Free → Personal → Team upgrades as
mailbox count and history needs grow. The MVP front-end (thread list,
conversation view, reply form, dark mode, keyboard shortcuts, pricing
page) is built; the app cannot yet take a payment or even sign anyone in.

## Primary Objective

**Ship EPIC-02 Monetization Plumbing.** Take the app from "anonymous demo"
to "a stranger on the internet can create an account, start a 14-day
Personal trial via Stripe Checkout, and convert to a paying customer on
day 15." This is the single largest dollar-value endeavor in the backlog
and the only one that turns the existing UI into revenue. Everything else
in the active epics (landing pages, IMAP polling, onboarding wizard)
either funnels into this or is meaningless without it.

## Milestones

1. **Auth foundation.** Spring Security email/password registration +
   login + remember-me; CSRF wired into the existing reply POST; a `User`
   entity owning `EmailThread` so the app is multi-tenant from day one.
2. **Stripe billing.** Subscription plans (Free / Personal / Team /
   Enterprise) wired to Stripe Checkout; webhook handler that mirrors
   subscription state into a local `Subscription` table; pricing page
   CTAs point at `/signup?plan=…` and complete the funnel.
3. **Trial + self-serve.** 14-day Personal trial (`trial_period_days=14`,
   no card at signup); Stripe customer portal link so users can
   upgrade/downgrade/cancel themselves.
4. **Plan limits.** Enforce mailbox count and thread-history cutoff per
   plan; inline upgrade modal when a Free user hits the limit.

## Done means

A test user signs up with email + password on a production deploy, picks
the Personal plan, completes Stripe Checkout in trial mode (no charge),
the webhook flips their local subscription to `trialing`, they connect a
mailbox and see threads, and on simulated day 15 Stripe charges the saved
card $9 and the webhook flips them to `active`. End-to-end, no human in
the loop after signup.
