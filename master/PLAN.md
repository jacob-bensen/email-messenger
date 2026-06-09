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
2. **Day-3 follow-up linking to the demo conversation.** Pending —
   second-tier nudge for signups still cold 72h after registration,
   re-using the stamp + opt-out plumbing but with a content variant
   that leads with the `/demo` link rather than the IMAP form.
3. **Day-7 last-chance "here's what you're missing".** Pending —
   final nudge for signups still cold one week in, with a
   trial-extension or downgrade-to-Free framing depending on plan
   intent captured at signup.
4. **Trial-end conversion email when paid trials expire.** Pending —
   `subscriptions.trial_ends_at` is already tracked; sweep the
   T-1 / T-0 days, send a conversion push, and surface
   conversion-from-trial as a separate column on `/admin/revenue`.

## Done means

A signup whose `created_at` is older than 24h and has no `MailAccount`
receives exactly one transactional activation email naming the
`/mailboxes/new` connect form (and a `/demo` preview link) and a
single unsubscribe link that kills the entire automated-marketing
channel via the existing `digest_email_preferences` opt-out token. A
follow-up sweep does not re-send to the same user. The post-EPIC-14
24h-cold-to-connected conversion rate, attributable per `utm_source`
on `/admin/revenue`, is visibly higher than the pre-EPIC-14 baseline.
