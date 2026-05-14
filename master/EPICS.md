# Epics

Strategic feature endeavors for MailIM. Each Epic groups related backlog
tasks and is rated for income impact. No more than 3 epics may be `[ACTIVE]`
at once.

Status legend: `[ACTIVE]` `[PLANNED]` `[COMPLETE]` `[PAUSED]`
Income impact: `LOW` `MEDIUM` `HIGH`

---

## EPIC-01 — Conversion Surface  [ACTIVE]  HIGH

**Goal**: Build the public-facing surface (landing page, pricing page, demo
mode, public roadmap, PWA manifest) so first-time visitors can experience the
product and convert without an account. Right now `/` redirects straight into
`/threads`, which means there is *zero* surface for organic traffic, ad
clicks, or social referrals to land on. Until this epic ships, no marketing
spend or distribution touchpoint can convert into paid users.

Child tasks (from INTERNAL_TODO.md):
- [GROWTH][M] SEO-friendly static landing page at `/`
- [GROWTH][S] Static pricing page at `/pricing`
- [GROWTH][S] Demo mode at `/demo` (sample threads, no auth)
- [GROWTH][S] Public roadmap page at `/roadmap`
- [GROWTH][S] PWA web app manifest + apple-touch-icon
- [UX][S] Thread list header `+ Add mailbox` dead-end fix

---

## EPIC-02 — Monetization Plumbing  [ACTIVE]  HIGH

**Goal**: Wire up the full path from "anonymous visitor" to "paying customer"
— user authentication, plan-limit enforcement, Stripe billing, customer
portal, trial, and the upgrade prompts that drive conversion. Without this
epic, the app cannot accept money. This is the single largest dollar-value
endeavor in the backlog.

Child tasks:
- [GROWTH][M] User registration & authentication (Spring Security)
- [GROWTH][S] Google OAuth SSO
- [GROWTH][M] Stripe billing integration (plans, checkout, webhook)
- [GROWTH][S] Stripe customer portal integration
- [GROWTH][S] 14-day free trial on Personal tier
- [GROWTH][S] Plan-limit enforcement
- [GROWTH][S] Annual/monthly billing toggle
- [GROWTH][S] Upgrade prompt at free-tier limit
- [GROWTH][S] In-app upgrade preview of locked features

---

## EPIC-03 — Mailbox Onboarding  [ACTIVE]  HIGH

**Goal**: Get a brand-new user from "I just signed up" to "I see my own email
threads as IM bubbles" with as little friction as possible. This includes
IMAP polling (the engine that imports mail automatically), the first-run
wizard, the unique `@mailaim.app` forwarding address, EML upload, and the
mailbox settings page. Users who never reach this aha-moment churn at the
top of the funnel.

Child tasks:
- [CORE][M] IMAP polling job (`@Scheduled`) behind a feature flag
- [GROWTH][M] First-run onboarding wizard
- [GROWTH][M] Email forwarding address `@mailaim.app`
- [GROWTH][S] EML file upload
- [UX][S] IMAP sync status indicator
- [UX][S] Mailbox settings page (resolves dead-end link)

---

## EPIC-04 — Engagement & Retention  [PLANNED]  MEDIUM

**Goal**: Drive daily/weekly re-engagement and reduce 30-day churn. Includes
unread tracking, snooze, archive, pin, search, and digest emails. These
features compound: each one is a reason to come back tomorrow.

Child tasks:
- [GROWTH][S] Unread thread tracking + badge
- [GROWTH][M] Full-text search (PostgreSQL tsvector)
- [GROWTH][S] Thread snooze
- [GROWTH][S] Thread archiving
- [GROWTH][S] Conversation pinning
- [GROWTH][M] Email digest notifications (daily/weekly)
- [GROWTH][S] Browser push notifications

---

## EPIC-05 — Differentiation & Upsell  [PLANNED]  MEDIUM

**Goal**: Build the features that gate Personal+ and Team tiers — the
reasons someone upgrades. AI summary, Slack/Discord webhooks, REST API,
shared inbox, send-later scheduling, thread export, labels.

Child tasks:
- [GROWTH][M] AI-generated thread summary (Claude API)
- [GROWTH][M] Slack/Discord webhook integration
- [GROWTH][M] REST API for Personal+ tier
- [GROWTH][M] Send-later scheduling for replies
- [GROWTH][M] Thread export (PDF/HTML)
- [GROWTH][M] Thread labels/tags
- [GROWTH][S] Outbound webhook on new message
- [GROWTH][M] Thread permalink sharing

---

## EPIC-06 — Distribution & Virality  [PLANNED]  MEDIUM

**Goal**: Build features that turn each user into a distribution touchpoint:
referrals, "Sent via MailIM" footer, in-app referral prompts, copy-as-
markdown, screenshot-friendly thread views.

Child tasks:
- [GROWTH][S] "Sent via MailIM" footer in Free-tier replies
- [GROWTH][S] Public referral link (1 month free on conversion)
- [GROWTH][S] In-app referral prompt
- [GROWTH][S] "Copy conversation as Markdown"

---

## EPIC-07 — Operations & Hardening  [PLANNED]  LOW

**Goal**: Make the app deployable, observable, and CI-tested. Until this
epic ships, every shipped feature is at risk of regressions and the app
cannot be hosted reliably.

Child tasks:
- [CORE][L] Integration tests with Testcontainers + GreenMail
- [CORE][M] Dockerfile + docker-compose.yml
- [CORE][S] GitHub Actions CI

---

## EPIC-08 — Polish & UX Refinement  [PLANNED]  LOW

**Goal**: Visual and interaction polish that doesn't directly drive
conversion but compounds with everything else: mobile layout, last-message
preview in the thread list, Gravatar avatar fallback, custom SMTP/from
address, reply signature.

Child tasks:
- [UX][M] Mobile layout pass (375px-wide screens)
- [UX][S] Thread list last-message-body preview
- [GROWTH][M] Gravatar + initials avatar fallback
- [GROWTH][S] Custom SMTP/from-address per user
- [GROWTH][S] Reply signature

---

## Completed Epics

_(none yet — first run that creates this file)_
