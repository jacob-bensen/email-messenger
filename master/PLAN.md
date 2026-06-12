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
subscriptions, with natural Free → Personal → Team upgrades and reclaim
revenue from win-back of canceled paid customers. Conversion funnel
(EPIC-02 through EPIC-16) and churn telemetry (EPIC-17) are code-complete;
live deploy is gated on Master ops (hosting, domain, Stripe live keys,
encryption secrets, Google OAuth credentials, demo video URL).

## Primary Objective

**Ship EPIC-18 operator-initiated win-back outreach so canceled paid
customers are a measurable reclaim channel, not a one-way exit.**
EPIC-17 turned cancellations into numbers the operator can see; EPIC-18
turns each row in the at-risk retention queue into an action. The
operator can click "Send win-back" on a canceled row, the customer
receives a reason-aware templated email through the existing
transactional path, the one-shot stamp prevents double-sends, and the
operator can later see how many of those emails actually reactivated a
subscription and how much MRR walked back through the door. Once the
loop closes, the at-risk queue stops being a list of regrets and starts
being a pipeline. This Objective ends when the operator can open
`/admin/revenue`, fire a win-back, and read the win-back-→-reactivation
conversion rate against the prior 30-day baseline in the same five
seconds the churn read takes today.

## Milestones

1. **Per-row "Send win-back" CTA + templated send + one-shot stamp.**
   `WinBackOutreachService` (in `com.emailmessenger.admin`) sends a
   reason-aware templated email through the existing `JavaMailSender`,
   gating on the subscription still being a canceled paid row, the
   `DigestEmailPreference` opt-out flag, and a null
   `last_win_back_email_sent_at`. A `POST /admin/retention/win-back`
   on `AdminRevenueController` drives the per-row button. Flyway V28
   adds the stamp column. The at-risk table swaps the action cell to
   a "Sent X ago" timestamp on re-render.
2. **Win-back conversion card on `/admin/revenue`.** New
   `WinBackConversionMetrics(Service)` reading from
   `SubscriptionRepository.findWinBackEmailedSince(...)`: emails sent,
   how many flipped back to `active` after the stamp, MRR recovered,
   and the same prior-30-day delta pattern the churn card uses. Tells
   the operator whether these emails are actually landing or whether
   the copy / pricing offer needs to change.
3. **Auto-suppress recovered rows + flag re-subscriptions.** When a
   subscription flips `canceled → active` after a win-back stamp,
   surface that as a "Recovered" badge in the at-risk queue (the row
   stays visible for context but loses the "Send win-back" CTA) and
   counts the row as a conversion in the M2 card. Closes the loop so
   the operator can tell on the same dashboard whether a click
   actually paid off.
4. **Operator weekly digest gains a win-back queue line + recovered
   tally.** Extend `AdminWeeklyDigestService` so the Monday morning
   email reads "Win-back queue: N un-emailed paid cancels this week"
   plus "Recovered after win-back: M (+$X MRR)" — pushes the action
   into the operator's inbox and pairs it with the proof it worked.

## Done means

An operator opens `/admin/revenue`, sees the at-risk retention row for a
just-canceled customer with a "Send win-back" button, clicks it, the
customer receives a reason-aware win-back email within seconds, the row
flips to "Sent X ago", and — when that customer re-subscribes — the
same page renders the row as "Recovered" while the win-back conversion
card increments its "Reactivated" + "MRR recovered" tallies. The Monday
operator digest carries both the un-emailed-queue size and the recovered
tally for the week. The operator decides whether to keep firing
win-backs, change the copy, or rebuild the pricing tier from the
dashboard alone.
