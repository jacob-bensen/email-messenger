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
mailbox count and history needs grow. EPICs 02–05 (Monetization, Mailbox
Onboarding, Deployability, Acquisition) are code-complete in
`claude_routine`; live deploy is gated on Master ops (hosting, domain,
Stripe live keys, encryption secrets).

## Primary Objective

**Ship EPIC-06 Launch readiness.** A Product Hunt visitor who lands on
the landing page and clicks "Start free" today reaches a working signup
and an empty inbox — but Stripe live-mode requires Privacy / Terms /
Refund pages plus a cookie-consent banner, the landing hero still shows a
static screenshot mock instead of a real demo, and the post-signup
experience drops a brand-new user onto an empty `/threads` with no guided
path to first value. This Objective ends when MailIM is ready to flip
from Stripe test mode to live and a cold visitor's first 60 seconds —
land, sign up, see the IM transform — is intentional from end to end.

## Milestones

1. **Legal pages + cookie consent.** `/privacy`, `/terms`, `/refund` are
   served from the app (content sourced from a configurable Markdown or
   HTML resource so Master can drop in Termly/Iubenda output without a
   code change), linked from the marketing footer and `/register`, and a
   dismissable cookie-consent banner appears on first visit for anonymous
   users. Required by Stripe before live-mode payments.
2. **First-touch in-app onboarding checklist on `/threads`.** Users with
   zero mailboxes (or zero imported threads) see a 3-step checklist —
   account created, connect inbox, see first conversation — with a primary
   CTA pointing at the next undone step, so a fresh signup never lands on
   a blank inbox with no next move.
3. **Demo video in the landing hero.** Replace the static screenshot mock
   in `landing.html` with a 60–90s Loom/YouTube embed (URL/ID injected
   via config so Master can swap it without a deploy), with a poster
   image fallback so first paint stays fast. _(Shipped 2026-06-05 — the
   embed wrapper renders whenever `MARKETING_LANDING_VIDEO_{PROVIDER,ID}`
   are set; static chat-bubble mock stays as the fallback when they're
   not. Open: Master must record + supply the actual demo URL — tracked
   under Launch / marketing in MASTER_ACTIONS.md.)_
4. **Trial-conversion nudge.** When a trialing user's trial ends in ≤3
   days, surface a more prominent in-app prompt (beyond the existing
   `trial-banner-urgent` strip) — e.g. a dismissable conversion modal on
   `/threads` with the chosen plan, price, and a one-click checkout —
   to push trial→paid conversion before expiry.

## Done means

`/privacy`, `/terms`, and `/refund` render real content (or
configurably-replaceable boilerplate); the cookie banner appears for
anonymous visitors and persists dismissal; a fresh signup with zero
mailboxes sees an onboarding checklist on `/threads` with a clear "next
step" CTA that progresses as they connect a mailbox and import their
first thread; the landing hero plays an embedded demo video instead of a
static mock; a trialing user with ≤3 days left gets a one-click
"Continue on <Plan>" conversion prompt; and the app is credibly ready to
flip Stripe to live mode.
