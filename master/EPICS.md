# Epics

Strategic feature endeavors that group related backlog tasks. Status:
[ACTIVE] (in flight) · [PLANNED] (queued) · [COMPLETE] · [PAUSED] (blocked).

At most 3 epics are [ACTIVE] at once.

---

## E1 — Auth & Account Foundation  [ACTIVE]

**Goal**: Ship user registration, login, sessions, and per-user data scoping.
Auth is the gating prerequisite for billing, multi-tenancy, custom SMTP,
signatures, archiving, pinning, and almost every other revenue feature in the
backlog. Until users can register, the product is single-tenant and
unmonetizable.

**Income impact**: HIGH (unblocks every revenue path)

**Child tasks** (from INTERNAL_TODO.md):
- [GROWTH][M] User registration and authentication (Spring Security + email/password + remember-me)
- [GROWTH][S] Add Google OAuth single sign-on
- [GROWTH][S] First-run onboarding wizard (depends on auth)
- [GROWTH][S] Custom SMTP/from-address per user (depends on auth)
- [GROWTH][S] Reply signature (depends on auth)
- [GROWTH][S] Thread archiving (depends on auth)
- [GROWTH][S] Conversation pinning (depends on auth)

---

## E2 — Marketing Surface & Conversion  [ACTIVE]

**Goal**: Build the no-auth-required public surface that turns organic traffic
and link-click visitors into trial users — landing page, pricing page, demo
mode, public roadmap. None of these depend on auth or billing infrastructure;
all are immediately shippable and immediately compounding (SEO + reduce
top-of-funnel drop-off).

**Income impact**: HIGH (top-of-funnel multiplier — every other epic's value
scales with this)

**Child tasks** (from INTERNAL_TODO.md):
- [GROWTH][M] SEO-friendly static landing page at /
- [GROWTH][S] Static pricing page at /pricing
- [GROWTH][S] Demo mode at /demo with pre-seeded sample threads
- [GROWTH][S] Public roadmap page at /roadmap
- [GROWTH][S] EML file upload (instant value preview, no IMAP setup)
- [GROWTH][S] PWA web app manifest

---

## E3 — Billing & Plan Enforcement  [ACTIVE]

**Goal**: Stripe billing, plan limits, and upgrade triggers wired end-to-end so
the freemium tier funnels into paid. This epic only delivers revenue once E1
(auth) is at least partly shipped, but its first sub-tasks (plan-limit logic,
upgrade UI) can be built against a hardcoded current-user once auth lands.

**Income impact**: HIGH (the literal revenue path)

**Child tasks** (from INTERNAL_TODO.md):
- [GROWTH][M] Stripe billing integration (subscriptions + checkout + webhook)
- [GROWTH][S] Stripe customer portal integration
- [GROWTH][S] Plan-limit enforcement (max mailboxes, history, threads)
- [GROWTH][S] Upgrade prompt at free-tier limit
- [GROWTH][S] In-app upgrade preview of locked features
- [GROWTH][S] Annual/monthly billing toggle
- [GROWTH][S] 14-day free trial on Personal tier

---

## E4 — Core Mail Pipeline  [PLANNED]

**Goal**: Make new mail arrive automatically (IMAP polling), and harden the
import pipeline with full integration tests. Without polling, users have to
manually trigger import, which kills retention. Without integration tests,
each release risks breaking the income-critical mail path.

**Income impact**: MEDIUM (retention; not a direct conversion lever, but
churn-protection for paid users)

**Child tasks** (from INTERNAL_TODO.md):
- [CORE][M] IMAP polling job (@Scheduled) behind a feature flag
- [CORE][L] Integration tests with Testcontainers + GreenMail
- [GROWTH][M] Email forwarding address (zero-friction onboarding alternative)
- [GROWTH][M] Email digest notifications

---

## E5 — Power-User & Team Features  [PLANNED]

**Goal**: Ship the differentiated features that drive Personal/Team plan
upgrades — search, AI summaries, webhooks, exports, labels, scheduling,
sharing. These are the upsell drivers; each one is a feature-gate that
justifies the next pricing tier.

**Income impact**: HIGH (upsell + ARPU expansion once paying users exist)

**Child tasks** (from INTERNAL_TODO.md):
- [GROWTH][M] Full-text search (Personal+ gate)
- [GROWTH][M] AI-generated thread summary (Personal+ gate, Claude API)
- [GROWTH][M] Slack/Discord webhook integration (Team gate)
- [GROWTH][M] Thread permalink sharing
- [GROWTH][M] Send-later scheduling for replies (Personal+ gate)
- [GROWTH][M] REST API for Personal+ tier
- [GROWTH][M] Thread export (PDF/HTML, Personal+ gate)
- [GROWTH][M] Thread labels/tags (Team gate)
- [GROWTH][S] Thread snooze
- [GROWTH][S] Browser push notifications
- [GROWTH][S] Outbound webhook trigger on new message (Team gate)
- [GROWTH][S] Unread thread tracking
- [GROWTH][S] "Copy conversation as Markdown"

---

## E6 — Distribution & Virality  [PLANNED]

**Goal**: Build the in-product mechanisms that turn each user into a
distribution channel — referral links, "Sent via MailIM" footer, in-app
referral prompts, shareable thread permalinks (overlap with E5).

**Income impact**: MEDIUM (CAC reduction; compounds over time)

**Child tasks** (from INTERNAL_TODO.md):
- [GROWTH][S] Shareable referral link ("Invite a teammate" → 1 month free)
- [GROWTH][S] "Sent via MailIM" branding footer for Free plan
- [GROWTH][S] In-app referral prompt after 10+ thread imports

---

## E7 — UX Polish & Mobile  [PLANNED]

**Goal**: Close the remaining UX gaps that erode trust — mobile responsive
pass, dead-end navigation links, last-message preview in thread list, IMAP
sync status indicator, Gravatar fallback. Each individually is small but
collectively they signal product maturity to evaluating users.

**Income impact**: MEDIUM (conversion friction)

**Child tasks** (from INTERNAL_TODO.md):
- [UX][S] Thread list last-message-body preview
- [UX][S] Header nav "+ Add mailbox" dead-end fix
- [UX][S] IMAP sync status indicator
- [UX][M] Mobile layout pass
- [GROWTH][M] Gravatar + initials avatar fallback

---

## E8 — Deployment & DevOps  [PLANNED]

**Goal**: Make the app deployable and CI-tested end-to-end so Master can
actually launch. Docker compose for self-hosted distribution, GitHub Actions
for build/test, and the deployment runbook items in TODO_MASTER.md.

**Income impact**: HIGH (literal blocker to launch / earning anything)

**Child tasks** (from INTERNAL_TODO.md):
- [CORE][M] Dockerfile + docker-compose.yml (app + postgres)
- [CORE][S] GitHub Actions CI: build, test, cache Maven deps

---

## Completed

_(none yet — first epic catalog is this session's deliverable)_
