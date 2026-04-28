# Epics — MailIM Roadmap

Format: `## [STATUS] EPIC-N: Name`
Statuses: `[ACTIVE]`, `[PLANNED]`, `[COMPLETE]`, `[PAUSED]`
Income impact: `LOW` / `MEDIUM` / `HIGH`

Each epic groups related backlog tasks. `[ACTIVE]` cap: 3.

---

## [ACTIVE] EPIC-1: Pre-Launch Conversion Funnel

**Goal**: Transform anonymous landing-page visitors into waitlist subscribers
and demo users so we have a warm audience the day Stripe + auth ship. Every
piece of copy, every CTA, every meta tag on a public page belongs here. This
is the single most income-relevant track until paid plans are live.

**Income impact**: HIGH (every signup is a future paying customer; the cost of
acquisition compounds once billing is on)

**Child tasks (from INTERNAL_TODO.md)**:
- Demo page SEO (h2, JSON-LD SoftwareApplication)
- OG/meta tags on threads.html, conversation.html, error.html
- SEO tags on legal pages (privacy, terms, refund)
- Waitlist success "Share this" CTA
- Demo "Share this demo" button
- Hero video/GIF embed slot
- Robots.txt + sitemap.xml
- Public roadmap page (/roadmap)
- EML file upload (lets visitors try with their own data — major aha moment)
- EML + .mbox drag-and-drop import zone on demo (HIGH conversion)
- .mbox file import (bulk archive import — removes IMAP barrier)
- Comparison landing page at /compare
- Waitlist position + launch ETA on success state
- Sticky "Get early access" CTA bar on /pricing
- JSON-LD FAQPage schema on /pricing
- Canonical URL on remaining public pages
- Testimonials on landing page and pricing page [UX]
- "Sent via MailIM" branding footer in outgoing replies (Free plan)
- PWA web app manifest (retention multiplier)
- Admin notification email on new waitlist signup
- Waitlist confirmation email (blocked on transactional email provider)
- Thread permalink sharing

---

## [ACTIVE] EPIC-2: Production Readiness & Trust

**Goal**: Get the app shippable, safe, and legally cleared to take payment in
the EU and US. Covers security headers, GDPR, performance, and the
infrastructure needed to deploy and observe MailIM in production. Without
this, no paid plan can launch.

**Income impact**: HIGH (blocks the entire revenue path; an EU user who hits
the site without a cookie banner is a lost lead and a regulatory risk)

**Child tasks (from INTERNAL_TODO.md)**:
- Cookie consent banner (HIGH legal/income — unblocks EU market)
- Content-Security-Policy header (requires inline scripts moved out first)
- Upgrade jsoup 1.17.2 → latest
- Attachment N+1 query (`@BatchSize(50)`)
- Replace legal notice placeholder copy in /privacy and /terms (Master action)
- Refund policy real legal copy before charging customers (Master action)

**Master-side blockers (TODO_MASTER.md)**:
- Deploy Docker Compose to Heroku/Render/Railway
- PostgreSQL hosted DB
- Domain registration
- Sentry error monitoring
- Transactional email provider (Postmark/Resend)

---

## [ACTIVE] EPIC-3: Core IM Reading Experience

**Goal**: Make the IM-style conversation view delightful enough that users
keep coming back daily. This is the product's core differentiator vs. Gmail
and Outlook — it has to feel obviously better. Covers thread list polish,
unread state, search, avatars, and the reading affordances power users expect.

**Income impact**: MEDIUM-HIGH (drives retention, which drives MRR; weak
retention here makes paid conversion impossible regardless of pricing copy)

**Child tasks (from INTERNAL_TODO.md)**:
- Basic thread search at GET /threads?q= (LIKE fallback)
- Add Gravatar + initials avatar fallback
- SSE live conversation refresh
- Add unread thread tracking + badge
- "Copy conversation as Markdown" button
- Keyboard shortcut `?` for help modal
- Thread list: last-message-body preview
- IMAP sync status indicator ("last synced X minutes ago")
- Mobile layout pass (375px)

---

## [PLANNED] EPIC-4: User Accounts & Multi-Tenancy

**Goal**: Ship Spring Security + email/password + remember-me so MailIM has
the concept of a "user" and can begin gating features by plan. This unlocks
every auth-gated growth task (referral, signature, OAuth, snooze, archive,
pinning, push, AI summary) and is the prerequisite for Stripe billing.

**Income impact**: HIGH (no auth → no billing → no MRR)

**Child tasks**: see "Auth-Gated Growth" section in INTERNAL_TODO.md.

---

## [PLANNED] EPIC-5: Stripe Billing & Plan Gating

**Goal**: Wire Stripe Checkout, customer portal, webhook handler, and the
plan-limit enforcement that turns subscriptions into recurring revenue.

**Income impact**: HIGH (this is literally the revenue switch)

**Child tasks**: see "Stripe-Gated Growth" section in INTERNAL_TODO.md.
**Master-side prerequisites**: Stripe account, products/prices, webhook secret.

---

## [PLANNED] EPIC-6: Integrations & API

**Goal**: REST API, outbound webhooks, Slack/Discord integrations, browser
extension, forwarding addresses. Each integration is both a feature and a
distribution channel.

**Income impact**: MEDIUM-HIGH (Team-tier upsell justification + organic
distribution loops)

**Child tasks**:
- REST API for Personal+ tier
- Outbound webhook trigger on new message
- Slack/Discord webhook integration
- Email forwarding address
- Thread export (PDF/HTML)
- Send-later scheduling for replies
- Full-text search (Postgres tsvector)
