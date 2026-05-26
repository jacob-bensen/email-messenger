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
mailbox count and history needs grow. EPIC-02 Monetization Plumbing is
code-complete (signup → Stripe trial → webhook-driven status → connect
mailbox → see threads); deploy + Stripe live keys remain Master ops work.

## Primary Objective

**Ship EPIC-03 Mailbox Onboarding.** Move the product from "a one-time
sync at connect" to "the inbox you connect on day 1 keeps showing new
mail on day 7 without anyone touching the app." A user is paying $9/mo
for an always-fresh chat-style inbox; if new mail stops arriving after
the initial sync, churn at the day-15 first-charge moment is near
certain. This Objective also covers the first-mailbox onboarding wizard
so the signup → trial → first-thread flow doesn't dump trial users on a
blank empty-state.

## Milestones

1. **Scheduled IMAP polling behind a feature flag.** `@Scheduled` job
   polls every connected mailbox on an interval, fetches messages newer
   than the persisted UID cursor, feeds them through
   `EmailImportService`, and advances the cursor; per-account failures
   recorded on the row instead of breaking the loop.
2. **First-mailbox onboarding wizard.** New trial user lands on a
   guided one-page connect flow (host presets for Gmail / iCloud /
   FastMail / Outlook, app-password help links, success/failure
   feedback) instead of the bare `/mailboxes/new` form.
3. **Manual "Sync now" trigger + sync status surfacing.** Per-mailbox
   button on `/mailboxes` that calls the same poll path on demand;
   `lastSyncedAt` and `lastSyncError` rendered with friendly relative
   timestamps and remediation hints.
4. ~~**Sane defaults + safety rails.** Polling interval pinned per plan
   tier (Free = 15 min, Personal+ = 5 min); jitter to avoid
   thundering-herd; circuit-breaker that suspends polling for an
   account after N consecutive failures with operator-visible state.~~
   Shipped 2026-05-26.

## Done means

A test user signs up, completes Stripe Checkout in trial mode, connects
a real Gmail mailbox through the wizard, sees the latest threads
immediately, then sends themselves a new email from another address —
within the configured poll interval that new message appears in the
MailIM inbox without any user action, and `lastSyncedAt` on
`/mailboxes` updates accordingly. If credentials expire or the IMAP
server becomes unreachable, the failure is visible on `/mailboxes`
with a clear next step.
