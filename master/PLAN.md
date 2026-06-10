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
count, history, and saved-search counts grow. EPICs 02–13 (Monetization
through Google OAuth signup) are code-complete in `claude_routine`; live
deploy is gated on Master ops (hosting, domain, Stripe live keys,
encryption secrets, Google OAuth credentials, demo video URL).

## Primary Objective

**Ship EPIC-14 activation drip — convert cold signups who never connect
a mailbox.** The funnel dashboard from EPIC-12 makes the next leak
visible: a meaningful share of post-EPIC-13 Google + email signups
never connect a mailbox, never accumulate threads, and silently churn.
The existing 7-day reengagement sweep is gated on unread-thread count,
so it specifically misses this cohort (zero threads → no unread → no
nudge → no email ever). EPIC-13 collapsed the password chasm; EPIC-14
collapses the IMAP-credentials chasm by reaching cold signups in their
own inbox with a "connect in 60 seconds" nudge plus a link to the live
`/demo` so they can see what they'd get before they hand over IMAP
credentials. This Objective ends when a signup who hasn't connected a
mailbox 24h after registering receives a single transactional nudge,
opting out kills the whole automated-marketing channel via the existing
unsubscribe token, and the rate at which 24h-cold signups eventually
connect a mailbox is visibly higher on the operator dashboard.

## Milestones

1. **Day-1 "connect your mailbox" activation email.** [shipped
   2026-06-09] Flyway V20 adds nullable `users.last_activation_nudge_sent_at`
   as a one-shot stamp. New `ActivationService` finds enabled signups
   whose `created_at` is older than `ACTIVATION_DELAY` (24h),
   `last_activation_nudge_sent_at IS NULL`, and have no `MailAccount`
   row, then composes a plain-text email pointing at `/mailboxes/new`
   and the public `/demo` and carrying the existing `digest_email_preferences`
   opt-out token so one unsubscribe link kills every automated channel.
   `ActivationScheduler` gates the recurring sweep behind
   `activation.enabled=true` and defaults to `0 30 13 * * ?` UTC so dev
   never emails by accident; the service stays in the context either
   way for manual/programmatic invocation. 7 service tests +
   2 feature-flag tests cover cohort selection, idempotency, opt-out,
   mailbox-already-connected exclusion, the 24h cool-off, and the email
   body content.
2. **Day-3 follow-up linking to the demo conversation.** [shipped
   2026-06-09] Flyway V21 adds nullable
   `users.last_activation_followup_sent_at` as a second one-shot stamp,
   tracked independently of the day-1 stamp. New
   `ActivationService.runActivationFollowupCycle` sweeps signups whose
   `created_at` is older than `ACTIVATION_FOLLOWUP_DELAY` (72h), have
   `last_activation_nudge_sent_at IS NOT NULL` (sequencing: day-1
   already fired), `last_activation_followup_sent_at IS NULL`, and no
   `MailAccount` row. Body leads with `/demo` (no signup, no credentials)
   and only mentions `/mailboxes/new` after — different framing for a
   cohort that didn't act on the day-1 IMAP-form CTA. Same
   `digest_email_preferences` opt-out token, so one unsubscribe still
   kills every automated channel. `ActivationScheduler` gains a second
   `@Scheduled` at `0 45 13 * * ?` UTC (override via
   `ACTIVATION_FOLLOWUP_CRON`) under the same `activation.enabled`
   flag. 7 new tests cover demo-lead body ordering, sequencing (no
   send before day-1), mailbox-connected exclusion, 72h cool-off,
   idempotency, opt-out, and cohort partitioning.
3. **Day-7 last-chance "here's what you're missing".** [shipped
   2026-06-10] Flyway V22 adds nullable
   `users.last_activation_lastchance_sent_at` as a third one-shot
   stamp, tracked independently of the day-1 and day-3 stamps. New
   `ActivationService.runActivationLastChanceCycle` sweeps signups
   whose `created_at` is older than `ACTIVATION_LAST_CHANCE_DELAY`
   (168h), have `last_activation_followup_sent_at IS NOT NULL`
   (sequencing: day-3 already fired — transitively requires day-1
   too), `last_activation_lastchance_sent_at IS NULL`, and no
   `MailAccount` row. Body branches on plan intent: paid-trial signups
   (Subscription row exists with PERSONAL/TEAM/ENTERPRISE plan) get
   "your 14-day trial clock is running" + a downgrade-to-Free
   `/billing` fallback; everyone else (no Subscription or FREE plan)
   gets "you picked Free, no trial clock, take your time". Both end
   with a `/demo` link and the existing `digest_email_preferences`
   opt-out token. `ActivationScheduler` gains a third `@Scheduled` at
   `0 0 14 * * ?` UTC (override via `ACTIVATION_LASTCHANCE_CRON`)
   under the same `activation.enabled` flag.
4. **Trial-end conversion email when paid trials expire.** [shipped
   2026-06-10] Flyway V23 adds nullable
   `subscriptions.last_trial_end_email_sent_at` as a one-shot stamp
   on the subscription row (not `users`, since the trial lifecycle
   belongs to the subscription and a future re-trial should re-open
   the cohort). New `TrialEndConversionService.runTrialEndCycle`
   sweeps `subscriptions` rows where `status='trialing'`, `plan IN
   (PERSONAL, TEAM)` (ENTERPRISE excluded — sales-led, matching the
   existing `TrialConversionNudgeService`), `trial_ends_at` lands
   inside the next 24h, and `last_trial_end_email_sent_at IS NULL`.
   Body leads with `/pricing` ("pick a plan to keep going"), then
   `/billing` (manage payment / downgrade to Free), then `/demo`,
   ending with the existing `digest_email_preferences` opt-out
   token so one unsubscribe still kills every automated channel.
   `TrialEndConversionScheduler` runs `0 30 14 * * ?` UTC (override
   via `TRIAL_END_CRON`) under a `trial-end.enabled=true` flag.
   New `TrialEndConversionMetricsService` powers a "Trial-end
   conversion — last 30 days" card on `/admin/revenue` with emails
   sent + converted-to-active + conversion rate, anchored on the
   one-shot stamp so the operator can compare against the
   pre-EPIC-14 baseline.

## Done means

A signup whose `created_at` is older than 24h and has no `MailAccount`
receives exactly one transactional activation email naming the
`/mailboxes/new` connect form (and a `/demo` preview link) and a
single unsubscribe link that kills the entire automated-marketing
channel via the existing `digest_email_preferences` opt-out token. A
follow-up sweep does not re-send to the same user. The post-EPIC-14
24h-cold-to-connected conversion rate, attributable per `utm_source`
on `/admin/revenue`, is visibly higher than the pre-EPIC-14 baseline.
