# Internal TODO

Format: [STATUS] [TAG] [SIZE] [EPIC] Description

- Statuses: TODO, IN-PROGRESS, BLOCKED  (DONE → moved to DONE_ARCHIVE.md)
- Tags: CORE, GROWTH, HEALTH, TEST-FAILURE, UX
- Sizes: S=<1h, M=1–3h, L=3h+ (split if possible)
- Epic IDs: see master/EPICS.md (E1–E8). UNASSIGNED → Role 1 to triage.

Priority order (top → bottom):
[TEST-FAILURE] → income-critical CORE → [UX] (conversion) → [HEALTH] →
[GROWTH] → [BLOCKED]

---

## Active

### Income-critical CORE / Auth foundation (E1, E3)

- [ ] TODO [GROWTH] [M] [E1] User registration and authentication
      (Spring Security + email/password + remember-me) — prerequisite for
      billing and multi-tenancy. THIS IS THE CRITICAL PATH.
- [ ] TODO [GROWTH] [M] [E3] Stripe billing integration: subscription plans
      (Free/Personal/Team), checkout flow, webhook handler for subscription
      events. Prerequisite: user auth (E1).
- [ ] TODO [GROWTH] [S] [E3] Stripe customer portal: self-service plan
      upgrade/downgrade/cancellation via Stripe Billing Portal. HIGH income
      impact. Prereq: Stripe billing.
- [ ] TODO [GROWTH] [S] [E3] Plan-limit enforcement: max mailboxes per plan,
      max thread history, show upgrade prompt at limit.
- [ ] TODO [GROWTH] [S] [E3] Upgrade prompt inline in thread list when user
      reaches free tier limit (500 threads or 1 mailbox); modal with plan
      comparison table. HIGH income impact.
- [ ] TODO [GROWTH] [S] [E3] In-app upgrade preview of locked features:
      disabled/blurred Team-tier features (Slack webhook, full-text search)
      with "Upgrade to unlock" CTA. HIGH income impact.
- [ ] TODO [GROWTH] [S] [E3] 14-day free trial on Personal tier
      (trial_period_days=14). HIGH income impact. Prereq: Stripe billing.
- [ ] TODO [GROWTH] [S] [E3] Annual/monthly billing toggle on settings page
      (the /pricing toggle already exists; this is the post-checkout side).
      Prereq: Stripe billing.
- [ ] TODO [GROWTH] [S] [E1] Google OAuth single sign-on: lower signup
      friction and auto-populate Gmail mailbox connection. HIGH income impact.
- [ ] TODO [GROWTH] [M] [E1] First-run onboarding wizard: guided "connect
      your first mailbox" flow with progress steps. HIGH income impact.
      Prereq: user auth.
- [ ] TODO [GROWTH] [S] [E1] Custom SMTP/from-address settings per user
      account. HIGH income impact. Prereq: user auth.
- [ ] TODO [GROWTH] [S] [E1] Reply signature: per-user configurable HTML/text
      signature appended to outgoing replies. MEDIUM income impact. Prereq:
      user auth.
- [ ] TODO [GROWTH] [S] [E1] Thread archiving: per-user archive state,
      /archived route. MEDIUM income impact. Prereq: user auth.
- [ ] TODO [GROWTH] [S] [E1] Conversation pinning: pin up to 3 threads to top
      of list. MEDIUM income impact. Prereq: user auth.

### Marketing surface — no auth required (E2)

- [ ] TODO [GROWTH] [M] [E2] SEO-friendly static landing page at / with
      features, pricing table, and CTA; serves organic traffic before users
      register. HIGH income impact.
- [ ] TODO [GROWTH] [S] [E2] Demo mode at /demo: pre-seed 2–3 realistic
      sample threads visible without an account. HIGH income impact.
- [ ] TODO [GROWTH] [M] [E2] SEO comparison pages: /compare/superhuman,
      /compare/hey, /compare/mimestream — feature-matrix + pricing snippet.
      HIGH income impact. No prerequisites.
- [ ] TODO [GROWTH] [S] [E2] Open Graph + Twitter Card metadata on /pricing
      and /: og:title, og:description, og:image, twitter:card meta tags so
      links render with preview when shared. MEDIUM income impact. No prereq.
- [ ] TODO [GROWTH] [S] [E2] EML file upload: upload a raw .eml file to seed
      threads instantly; useful for demos. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] [E2] PWA web app manifest + apple-touch-icon to
      enable "Add to Home Screen". MEDIUM income impact.
- [ ] TODO [GROWTH] [S] [E2] Public roadmap page at /roadmap: static HTML
      listing upcoming features. MEDIUM income impact.
- [ ] TODO [GROWTH] [M] [E2] /blog index + 3 seed SEO posts at /blog.
      Compounds organic traffic. MEDIUM income impact.

### Core Mail Pipeline (E4)

- [ ] TODO [CORE] [M] [E4] IMAP polling job (@Scheduled) behind a feature
      flag. Required for retention — without it, users must manually trigger
      import.
- [ ] TODO [CORE] [L] [E4] Integration tests with Testcontainers (Postgres) +
      GreenMail (SMTP/IMAP). [L] — consider splitting into "Postgres-only"
      and "GreenMail SMTP" sub-tasks.
- [ ] TODO [GROWTH] [M] [E4] Email forwarding address: assign each user a
      unique @mailaim.app address; auto-imports forwarded emails — avoids
      IMAP credential setup. HIGH income impact. Prereq: user auth.
- [ ] TODO [GROWTH] [M] [E4] Email digest notifications (daily/weekly summary
      of unread threads). Re-engagement / churn-protection.

### Power-user & Team upsell (E5)

- [ ] TODO [GROWTH] [M] [E5] Full-text search across threads (PostgreSQL
      tsvector). Personal/Team upgrade gate.
- [ ] TODO [GROWTH] [M] [E5] AI-generated thread summary (Claude API).
      Personal+ tier gate; unique differentiator. HIGH income impact.
      Prereq: user auth + ANTHROPIC_API_KEY.
- [ ] TODO [GROWTH] [M] [E5] Slack/Discord webhook integration on new email.
      Team plan gate. HIGH income impact.
- [ ] TODO [GROWTH] [M] [E5] Thread permalink sharing: /share/{token}
      read-only link. HIGH income impact (viral touchpoint).
- [ ] TODO [GROWTH] [M] [E5] Send-later scheduling for replies. Personal/Team
      gate. HIGH income impact.
- [ ] TODO [GROWTH] [M] [E5] REST API for Personal+ tier: JSON API for
      thread/message/reply ops. HIGH income impact.
- [ ] TODO [GROWTH] [M] [E5] Thread export (PDF/HTML). Personal/Team gate.
      MEDIUM income impact.
- [ ] TODO [GROWTH] [M] [E5] Thread labels/tags. Team gate. MEDIUM income.
- [ ] TODO [GROWTH] [S] [E5] Thread snooze: re-surface at a set time.
- [ ] TODO [GROWTH] [S] [E5] Browser push notifications via Web Push API.
- [ ] TODO [GROWTH] [S] [E5] Outbound webhook trigger on new message. Team
      plan gate. Prereq: IMAP polling.
- [ ] TODO [GROWTH] [S] [E5] Unread thread tracking: mark-as-read on view,
      unread count badge in thread list.
- [ ] TODO [GROWTH] [S] [E5] "Copy conversation as Markdown" button.

### Distribution & Virality (E6)

- [ ] TODO [GROWTH] [S] [E6] Shareable referral link: "Invite a teammate" →
      1 month free on conversion.
- [ ] TODO [GROWTH] [S] [E6] "Sent via MailIM" branding footer for Free plan
      outgoing replies.
- [ ] TODO [GROWTH] [S] [E6] In-app referral prompt after 10+ thread imports.
- [ ] TODO [GROWTH] [S] [E3] Exit-intent modal on /pricing — JS detects
      cursor leaving viewport top, fires one-time modal with extended-trial
      CTA. HIGH income impact. Prereq: Stripe trial logic.

### UX & Polish (E7)

- [ ] TODO [UX] [S] [E7] Site footer fragment — Thymeleaf fragment included
      in threads/conversation/pricing templates with /pricing /roadmap /blog
      links and "Trusted by N users" placeholder; closes the dead-end at the
      bottom of every page.
- [ ] TODO [UX] [S] [E7] Thread list last-message preview (first 80 chars)
      below the subject line — requires denormalizing or join.
- [ ] TODO [UX] [S] [E1] Header nav "+ Add mailbox" → /settings/mailboxes
      currently 404s; implement settings page (or redirect to onboarding).
- [ ] TODO [UX] [S] [E4] IMAP sync status indicator: "last synced X minutes
      ago" in thread list header.
- [ ] TODO [UX] [M] [E7] Mobile layout pass: thread list and conversation
      bubble view on 375px-wide screens.
- [ ] TODO [UX] [S] [E7] Reply success flash auto-dismiss after 4s + close
      (×) button on .alert-success in conversation.html.
- [ ] TODO [UX] [S] [E2] /pricing noscript fallback: `<noscript>` block hides
      the billing toggle and shows "monthly pricing shown — annual saves 16%".
- [ ] TODO [GROWTH] [M] [E7] Gravatar + initials avatar fallback for
      Participant display (initials() helper already on Participant entity).

### Infrastructure / Deploy (E8)

- [ ] TODO [CORE] [M] [E8] Dockerfile + docker-compose.yml (app + postgres).
- [ ] TODO [CORE] [S] [E8] GitHub Actions CI: build, test, cache Maven deps.

---

## Blocked

_(none)_

---

DONE items archived to `master/DONE_ARCHIVE.md`. Older items: see CHANGELOG.md.
