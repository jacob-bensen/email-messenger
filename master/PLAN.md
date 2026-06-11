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
subscriptions, with natural Free → Personal → Team upgrades. The
conversion funnel (EPIC-02 through EPIC-16) is code-complete; live
deploy is gated on Master ops (hosting, domain, Stripe live keys,
encryption secrets, Google OAuth credentials, demo video URL).

## Primary Objective

**Ship EPIC-17 churn telemetry so the Team plan's $29/mo unit
economics are measurable.** The dashboard at `/admin/revenue` has
acquisition (funnel, source, onboarding) and upgrade (Team-plan
adoption) cards but no churn surface — the operator currently can't
tell if MRR growth is being chewed up by cancellations, can't see
per-plan churn (a single Team cancel costs ~3 Personal cancels), and
can't see whether retention work is landing. EPIC-17 closes that gap
end-to-end: an operator-readable churn card that pairs raw cancellation
counts with lost monthly-equivalent revenue, a gross-revenue-churn rate
that compares lost MRR against the MRR active at the start of the
window, a per-plan breakdown so a Team-plan cancel reads differently
from a Personal cancel, and a prior-30-day baseline so the rate is
read as a trend rather than a single number. This Objective ends when
the operator can open `/admin/revenue` and answer "is the Team plan
retaining better month-over-month?" in five seconds.

## Milestones

1. **Churn & MRR-retention card — last 30 days.** New `ChurnMetrics`
   record + `ChurnMetricsService` in `com.emailmessenger.admin` reading
   from `SubscriptionRepository.findCanceledBetween(from, to)`. Counts
   canceled subscribers, sums monthly-equivalent lost MRR (and ARR),
   computes gross revenue churn rate as `lostMrr / (currentMrr +
   lostMrr)`, breaks the cancellation cohort down by plan, and pairs
   each metric with the prior-30-day counterpart so the delta is
   visible. Wired into `AdminRevenueController` + rendered on
   `templates/admin/revenue.html`.
2. **Cancellation reason capture at the point of cancel.** Add a
   one-question reason picker on the Stripe-Portal-return path (or an
   in-app pre-cancel step) so each `subscriptions.canceled_at` event
   carries a stable enum (`too_expensive` / `missing_feature` /
   `switching` / `temporary` / `other`). Persist on the Subscription row
   so the operator can see *why* the Team-plan cancels happened, not
   just how many.
3. **At-risk retention queue — trial-end conversions that lapsed.** A
   new `/admin/retention` page (or a card on `/admin/revenue`) listing
   the last N subscriptions that flipped `active` → `canceled` inside
   the window, with their plan, cadence, acquisition source, and the
   recorded cancellation reason. This is the one-click "who do I email
   to win back" list.
4. **Operator weekly digest gains the per-plan churn line.** Extend
   `AdminWeeklyDigestService` so the Monday morning email reads
   "Personal: N canceled (-$X MRR); Team: N canceled (-$Y MRR);
   Enterprise: …" — pushes the retention number into the operator's
   inbox instead of relying on them visiting the dashboard.

## Done means

A Team-plan cancellation shows up in the "Churn & MRR retention" card
within minutes of the Stripe webhook landing, the lost MRR matches the
cents that walked out, the prior-30-day delta reads "▼ X% vs. prior 30
days" when retention is improving, the per-plan breakdown row for Team
moves independently of Personal, and the Monday operator digest
includes per-plan churn dollars. The operator can decide whether to
build a retention feature or a pricing change from the dashboard
alone.
