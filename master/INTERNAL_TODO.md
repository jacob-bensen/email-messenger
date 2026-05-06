# Internal TODO

Format: `[STATUS] [TAG] [SIZE] (EPIC-ID) Description`

- Statuses: `TODO`, `IN-PROGRESS`, `DONE`, `BLOCKED`
- Tags: `CORE`, `GROWTH`, `HEALTH`, `TEST-FAILURE`, `UX`
- Sizes: `S` (< 2h), `M` (2–4h), `L` (4–8h, consider splitting)
- Every task is assigned an Epic ID from `EPICS.md` or tagged `UNASSIGNED`.

Priority order: `TEST-FAILURE` → income-critical features → `UX` (conversion)
→ `HEALTH` → `GROWTH` → `BLOCKED` (bottom).

---

## TEST-FAILURE
_(none open)_

---

## Income-critical (path-to-revenue)

These are the features that gate the first paid customer. Ship in this order.

- [ ] TODO [GROWTH] [M] (EPIC-02) User registration and authentication —
      Spring Security email/password + remember-me. Prerequisite for Stripe,
      multi-tenancy, and most plan-gated features.
- [ ] TODO [GROWTH] [M] (EPIC-02) Stripe billing integration — subscription plans
      (Free/Personal/Team), checkout flow, webhook handler for subscription events.
- [ ] TODO [GROWTH] [S] (EPIC-02) Stripe customer portal integration — self-service
      plan upgrade/downgrade/cancellation. Reduces churn from users who can't
      self-serve. Prereq: Stripe billing.
- [ ] TODO [GROWTH] [S] (EPIC-02) 14-day free trial on Personal tier — set
      `trial_period_days=14` on the Stripe Personal price; no credit card at signup.
      Prereq: Stripe billing.
- [ ] TODO [GROWTH] [S] (EPIC-02) Plan-limit enforcement — max mailboxes per plan,
      max thread history, show upgrade prompt at limit.
- [ ] TODO [GROWTH] [S] (EPIC-02) Annual/monthly billing toggle on pricing/settings —
      monthly vs annual pricing with "Save 16%" label; higher LTV per conversion.
      Prereq: Stripe billing. (UI already shipped on `/pricing` Run #9.)
- [ ] TODO [GROWTH] [S] (EPIC-02) Upgrade prompt inline in thread list when user
      reaches free-tier limit (500 threads or 1 mailbox); modal with plan
      comparison.
- [ ] TODO [GROWTH] [S] (EPIC-02) In-app upgrade preview of locked features — show
      blurred Team-tier features (Slack webhook, full-text search) with
      "Upgrade to unlock" CTA.
- [ ] TODO [GROWTH] [S] (EPIC-02) Add-on extra mailbox at $3/mo — expansion revenue
      without forcing a tier upgrade. Prereq: Stripe billing.
- [ ] TODO [CORE] [M] (EPIC-03) IMAP polling job (`@Scheduled`) behind a feature
      flag — engine that auto-imports mail. Without it, programmatic import is
      the only data source.
- [ ] TODO [GROWTH] [M] (EPIC-03) First-run onboarding wizard — guided
      "connect your first mailbox" flow with progress steps; reduces signup-to-value
      drop-off.
- [ ] TODO [GROWTH] [M] (EPIC-03) Email forwarding address — assign each user a
      unique `@mailaim.app` address; any email forwarded there is auto-imported.
      Zero-friction onboarding path; avoids IMAP credential setup.
- [ ] TODO [GROWTH] [S] (EPIC-03) EML file upload — let users upload a raw `.eml`
      to seed threads instantly; useful for demos and offline testing.

---

## UX (conversion-affecting)

- [ ] TODO [UX] [S] (EPIC-01) Pricing page CTAs: "Start 14-day free trial" buttons
      currently link to `/threads`. Once auth + Stripe checkout ship, point them at
      `/signup?plan=personal` and `/signup?plan=team` so the funnel actually completes.
- [ ] TODO [UX] [S] (EPIC-01) Pricing page footer: add `/privacy`, `/terms`,
      `/refund-policy` links once those pages exist (Stripe go-live blocker).
- [ ] TODO [UX] [S] (EPIC-01) Conversation page header: add a sticky `Pricing` link
      so a free-tier user inside the inbox always has a one-click upgrade affordance.
- [ ] TODO [UX] [S] (EPIC-03) Thread list header `+ Add mailbox` link points to
      `/settings/mailboxes` which doesn't exist; build the mailbox settings page
      (or redirect to onboarding once auth ships).
- [ ] TODO [UX] [S] (EPIC-03) IMAP sync status indicator — "last synced X minutes ago"
      in the thread list header.
- [ ] TODO [UX] [S] (EPIC-08) Thread list last-message-body preview (first 80 chars)
      below the subject line — requires denormalizing a `last_message_preview` column
      or a query join.
- [ ] TODO [UX] [M] (EPIC-08) Mobile layout pass: usable on 375px-wide screens;
      bubbles must not overflow; also hide/collapse `.kbd-hint` below 640px.

---

## HEALTH

- [ ] TODO [HEALTH] [S] (EPIC-02) CSRF protection for `POST /threads/{id}/reply`
      and any future POST endpoints once Spring Security is added: include
      `<input type="hidden" name="_csrf" th:value="${_csrf.token}"/>` (or use
      Thymeleaf's automatic Spring Security dialect) in `conversation.html` and
      every form template. Currently the project has no Spring Security on the
      classpath so CSRF isn't enforced — but the moment auth is added, existing
      POSTs will start returning 403 unless this is wired together. Pre-emptive
      flag so EPIC-02 doesn't ship a broken reply form.
- [ ] TODO [HEALTH] [S] (EPIC-07) Audit `/h2-console` exposure: it's currently
      enabled under the `dev` profile only (`spring.h2.console.enabled: true` is
      inside the `on-profile: dev` block). Add a CI check or production smoke
      test that confirms `/h2-console` returns 404 in the prod profile, so a
      misconfigured `SPRING_PROFILES_ACTIVE` can never silently expose the DB
      browser in production.

---

## GROWTH (non-income-critical, retention/expansion/distribution)

### EPIC-01 Conversion Surface
- [ ] TODO [GROWTH] [M] (EPIC-01) SEO-friendly static landing page at `/` — features,
      pricing CTA. Currently `/` redirects to `/threads`.
- [ ] TODO [GROWTH] [S] (EPIC-01) Demo mode at `/demo` — pre-seed 2–3 sample threads
      visible without an account; lets visitors experience the IM view.
- [ ] TODO [GROWTH] [S] (EPIC-01) Public roadmap page at `/roadmap` — static HTML
      listing upcoming features; reduces "is this abandoned?" churn.
- [ ] TODO [GROWTH] [S] (EPIC-01) PWA web app manifest + apple-touch-icon — enables
      "Add to Home Screen" on iOS/Android; PWA installs ~3× higher 30-day retention.
- [ ] TODO [GROWTH] [S] (EPIC-01) Exit-intent email capture modal on `/pricing`
      and `/`: detect close/back intent, capture email to a `signup_interest` table
      or ConvertKit/Mailchimp.
- [ ] TODO [GROWTH] [S] (EPIC-01) Public stats page at `/stats` — server-rendered
      live counters as a trust signal; no third-party widget.

### EPIC-02 Monetization Plumbing
- [ ] TODO [GROWTH] [S] (EPIC-02) Google OAuth single sign-on — lower signup friction;
      auto-populate Gmail mailbox connection.

### EPIC-04 Engagement & Retention
- [ ] TODO [GROWTH] [S] (EPIC-04) Unread thread tracking — mark-as-read on view,
      unread badge in thread list.
- [ ] TODO [GROWTH] [M] (EPIC-04) Full-text search across threads (PostgreSQL
      tsvector) — Personal/Team upgrade gate.
- [ ] TODO [GROWTH] [S] (EPIC-04) Thread snooze — re-surface at a set time;
      reduces inbox anxiety.
- [ ] TODO [GROWTH] [S] (EPIC-04) Thread archiving — `Archive` action; `/archived`
      route; per-user state. Prereq: user auth.
- [ ] TODO [GROWTH] [S] (EPIC-04) Conversation pinning — pin up to 3 threads;
      per-user state. Prereq: user auth.
- [ ] TODO [GROWTH] [M] (EPIC-04) Email digest notifications (daily/weekly summary
      of unread threads) — re-engagement driver.
- [ ] TODO [GROWTH] [S] (EPIC-04) Browser push notifications via Web Push API —
      requires service worker.
- [ ] TODO [GROWTH] [M] (EPIC-04) Auto-categorize threads (Newsletter / Personal /
      Work) via List-Id header + sender-domain heuristics; show category chip + filter.

### EPIC-05 Differentiation & Upsell
- [ ] TODO [GROWTH] [M] (EPIC-05) AI-generated thread summary — one-sentence summary
      per thread in the list; Claude API; Personal+ tier gate. Prereq: auth +
      ANTHROPIC_API_KEY.
- [ ] TODO [GROWTH] [M] (EPIC-05) Smart reply suggestions via Claude API — 2–3 short
      suggested replies; one-click insert; Personal+ tier gate. Prereq: auth +
      ANTHROPIC_API_KEY.
- [ ] TODO [GROWTH] [M] (EPIC-05) Slack/Discord webhook integration — POST to a
      configured channel on new email arrival; Team plan gate.
- [ ] TODO [GROWTH] [M] (EPIC-05) REST API for Personal+ tier — JSON API for thread/
      message/reply operations; enables Zapier/Make integrations.
- [ ] TODO [GROWTH] [M] (EPIC-05) Send-later scheduling for replies — Personal/Team
      gate; useful for async remote workers across time zones.
- [ ] TODO [GROWTH] [M] (EPIC-05) Thread export (PDF/HTML) — Personal/Team gate;
      useful for freelancers and support teams.
- [ ] TODO [GROWTH] [M] (EPIC-05) Thread labels/tags — user-defined labels;
      Team plan gate.
- [ ] TODO [GROWTH] [S] (EPIC-05) Outbound webhook trigger on new message —
      simplified Zapier/Make integration; Team plan gate. Prereq: IMAP polling.
- [ ] TODO [GROWTH] [M] (EPIC-05) Thread permalink sharing — generate a shareable
      read-only `/share/{token}` link; viral touchpoint.

### EPIC-06 Distribution & Virality
- [ ] TODO [GROWTH] [S] (EPIC-06) "Sent via MailIM" branding footer in outgoing
      replies for Free plan users.
- [ ] TODO [GROWTH] [S] (EPIC-06) Public referral link "Invite a teammate" — awards
      1 month free on conversion.
- [ ] TODO [GROWTH] [S] (EPIC-06) In-app referral prompt — after 10+ threads imported,
      show "Loving MailIM? Invite a colleague" modal.
- [ ] TODO [GROWTH] [S] (EPIC-06) "Copy conversation as Markdown" — one-click copy
      of full thread as Markdown to clipboard.

### EPIC-08 Polish & UX Refinement
- [ ] TODO [GROWTH] [M] (EPIC-08) Gravatar + initials avatar fallback — initials()
      helper already on `Participant`.
- [ ] TODO [GROWTH] [S] (EPIC-08) Custom SMTP/from-address per user account — replies
      appear as the user's own identity. Prereq: user auth.
- [ ] TODO [GROWTH] [S] (EPIC-08) Reply signature — per-user configurable signature
      appended to outgoing replies. Prereq: user auth.

---

## Infrastructure

- [ ] TODO [CORE] [L] (EPIC-07) Integration tests with Testcontainers (Postgres) +
      GreenMail (SMTP/IMAP).
- [ ] TODO [CORE] [M] (EPIC-07) Dockerfile + docker-compose.yml (app + postgres).
- [ ] TODO [CORE] [S] (EPIC-07) GitHub Actions CI: build, test, cache Maven deps.

---

## BLOCKED
_(none — all blockers tracked in `TODO_MASTER.md` are external/Master actions)_
