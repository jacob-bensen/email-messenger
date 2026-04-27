# Internal TODO

Format: [STATUS] [TAG] [SIZE] Description

Statuses: TODO, IN-PROGRESS, DONE, BLOCKED
Tags: CORE, GROWTH, HEALTH, TEST-FAILURE, UX
Sizes: S=< 2h, M=2-4h, L=4-8h

---

## Active / High Priority

### Infrastructure (prerequisite for Dockerfile/CI)

- [ ] TODO [CORE] [M] Dockerfile + docker-compose.yml (app + postgres)
- [ ] TODO [CORE] [S] GitHub Actions CI: build, test, cache Maven deps
- [ ] TODO [CORE] [L] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP)

### No-Prerequisite Growth (ship next, highest ROI)

- [ ] TODO [GROWTH] [M] SEO-friendly landing page at /: the current / redirects to /threads (empty state for new visitors). Replace with a marketing page: hero, feature grid, pricing table preview, "Join waitlist" CTA. HIGH income impact — the entry point for all organic and social traffic.
- [ ] TODO [GROWTH] [S] Waitlist confirmation email: send a "you're on the list" transactional email immediately after a successful waitlist signup using Spring Mail (already in deps); keeps leads warm, confirms deliverability, includes /demo link. HIGH income impact. Prerequisite: waitlist (done ✓), transactional email provider credentials (see TODO_MASTER.md).
- [ ] TODO [GROWTH] [S] Waitlist count social proof: show live "Join X others on the waitlist" count on the waitlist page via WaitlistEntryRepository.count(); FOMO driver that improves form conversion. MEDIUM impact. Prerequisite: waitlist (done ✓).
- [ ] TODO [GROWTH] [M] Thread permalink sharing: generate a shareable read-only link to a thread view (e.g. /share/{token}); viral touchpoint, HIGH income impact.
- [ ] TODO [GROWTH] [M] .mbox file import: upload a raw .mbox archive (Google Takeout / Thunderbird export) to import all threads in bulk; removes IMAP credential requirement for first-time users. HIGH impact, no prerequisites.
- [ ] TODO [GROWTH] [S] Demo page SEO: add keyword-rich h2 sub-heading, feature bullet list, and JSON-LD SoftwareApplication schema to /demo; rank for "email as chat app" searches. MEDIUM impact. Prerequisite: demo page (done ✓).
- [ ] TODO [GROWTH] [S] Open Graph + meta description tags on threads.html, conversation.html, and error.html: add `og:title`, `og:description`, `og:type`, `<meta name="description">`; improves social-share previews. MEDIUM impact. (waitlist.html, pricing.html, demo.html already have OG tags.)
- [ ] TODO [GROWTH] [S] Social proof section on pricing page: 2–3 short testimonials (placeholder copy) below the plan cards; highest-leverage conversion booster on a pricing page. MEDIUM impact. No prerequisites.
- [ ] TODO [GROWTH] [S] Basic thread search at GET /threads?q=: JPA LIKE query on email_threads.subject and participants.email; unblocks search use case. MEDIUM impact, no prerequisites.
- [ ] TODO [GROWTH] [M] Add Gravatar + initials avatar fallback for Participant display in conversation view. MEDIUM impact.
- [ ] TODO [GROWTH] [S] Robots.txt + sitemap.xml: single controller serving /robots.txt (Allow: /, /demo, /pricing, /waitlist, /roadmap; Disallow: /threads, /h2-console) and /sitemap.xml listing all public routes; submit sitemap to Google Search Console. LOW impact individually, HIGH SEO leverage long-term.
- [ ] TODO [GROWTH] [S] Public roadmap page at /roadmap: static HTML listing upcoming features with rough ETA; reduces "is this abandoned?" churn. MEDIUM impact.
- [ ] TODO [GROWTH] [S] EML file upload: upload a raw .eml file to seed threads; useful for demos and offline testing. MEDIUM impact.
- [ ] TODO [GROWTH] [S] PWA web app manifest: manifest.json + apple-touch-icon; installs as PWA → 3× higher 30-day retention. MEDIUM impact.
- [ ] TODO [GROWTH] [M] SSE live conversation refresh: Spring SseEmitter pushes "new-message" event to the open conversation page when ImapPollingJob imports new emails; makes app feel real-time. MEDIUM impact. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [GROWTH] [S] Add unread thread tracking: mark-as-read on view, unread count badge in thread list. MEDIUM impact.
- [ ] TODO [GROWTH] [S] In-app upgrade preview of locked features: show disabled/blurred Team-tier features with "Upgrade to unlock" CTA. HIGH income impact.
- [ ] TODO [GROWTH] [S] "Copy conversation as Markdown" button: one-click copy of full thread as Markdown to clipboard. MEDIUM impact.
- [ ] TODO [GROWTH] [S] Outbound webhook trigger on new message: POST to configured URL when new thread message arrives (Zapier/Make integration); Team plan gate. MEDIUM impact. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [GROWTH] [S] Keyboard shortcut `?` to show help modal listing all keyboard shortcuts (j/k/Enter/r/Esc); power-user delight, retention driver. LOW-MEDIUM impact.
- [ ] TODO [GROWTH] [S] Canonical URL `<link rel="canonical">` on all public pages; prevents duplicate-content SEO penalties. LOW impact, no prerequisites.
- [ ] TODO [GROWTH] [S] "Sent via MailIM" branding footer in outgoing replies for Free plan users; disabled for Personal+. MEDIUM impact.

### UX

- [ ] TODO [UX] [S] Dark-mode legal notice box (.legal-notice in /privacy and /terms): hardcoded yellow colors are unreadable in dark mode; add @media (prefers-color-scheme: dark) overrides. [S]
- [ ] TODO [UX] [S] Thread list: show last-message-body preview (first 80 chars) below subject line — denormalize via query or add last_message_preview column to email_threads. MEDIUM impact.
- [ ] TODO [UX] [S] Thread list "+ Add mailbox" nav link points to /settings/mailboxes (404); update to redirect to auth/onboarding once user auth ships. [BLOCKED] until user auth.
- [ ] TODO [UX] [S] IMAP sync status indicator: show "last synced X minutes ago" in thread list header. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [UX] [M] Mobile layout pass: ensure thread list and conversation view are usable on 375px screens; bubbles must not overflow viewport.
- [ ] TODO [UX] [S] Replace legal notice placeholder in /privacy and /terms with real legal copy before accepting any payments or EU users. No code needed — requires legal content generation (see TODO_MASTER.md).

### Health

- [ ] TODO [HEALTH] [S] Attachment N+1 query: Message.attachments loaded lazily per message; add @BatchSize(size=50) to Message.attachments. Low priority until threads with many attachments are common.
- [ ] TODO [HEALTH] [S] Add CSRF protection when Spring Security ships: CSRF filter must be enabled (default) on the reply and waitlist POST endpoints. [BLOCKED] until user auth task starts.
- [ ] TODO [HEALTH] [S] Rate-limit POST /threads/{id}/reply and POST /waitlist to prevent form abuse; implement per-IP or per-user rate limiting. [BLOCKED] until auth ships (per-user limit) or via nginx/Cloudflare (per-IP, no code needed).

### Auth-Gated Growth (implement user auth first, then these unlock)

- [ ] TODO [GROWTH] [M] User registration and authentication (Spring Security + email/password + remember-me) — prerequisite for billing, multi-tenancy, and all auth-gated features below.
- [ ] TODO [GROWTH] [M] First-run onboarding wizard: guided "connect your first mailbox" flow after signup; reduces signup-to-value drop-off. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Add Google OAuth single sign-on: lower signup friction, auto-populate Gmail mailbox. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Custom SMTP/from-address settings per user: configure "From" email for outgoing replies. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] AI-generated thread summary: one-sentence summary per thread; Claude API; Personal+ tier gate. HIGH impact (unique differentiator). Prerequisite: user auth + ANTHROPIC_API_KEY.
- [ ] TODO [GROWTH] [S] Reply signature: per-user configurable HTML/text signature appended to replies. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Referral link "Invite a teammate" — awards 1 month free on conversion. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] In-app referral prompt: after user imports 10+ threads show "Loving MailIM? Invite a colleague" modal. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] Email digest notifications (daily/weekly unread thread summary) — re-engagement driver. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] Thread labels/tags: user-defined labels (e.g. "Client", "Urgent"); Team plan gate. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Thread snooze: re-surface thread at a set time. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Thread archiving: "Archive" action per thread; /archived route. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Conversation pinning: pin up to 3 threads to top of list; per-user state. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Browser push notifications via Web Push API: notify on new email in watched thread. Requires service worker. Prerequisite: user auth.

### Stripe-Gated Growth (implement user auth + Stripe billing first)

- [ ] TODO [GROWTH] [M] Add Stripe billing integration: subscription plans (Free/Personal/Team), checkout flow, webhook handler. HIGH income impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Stripe customer portal integration: self-service upgrade/downgrade/cancellation. HIGH impact. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] Plan-limit enforcement: max mailboxes per plan, max thread history, upgrade prompt at limit. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] Upgrade prompt inline in thread list when user hits free tier limit; plan comparison modal. HIGH impact. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] Annual/monthly billing toggle on pricing/settings page with "Save 16%" label. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] 14-day free trial on Personal tier: trial_period_days=14, no credit card at signup. HIGH impact. Prerequisite: Stripe billing.

### Larger Post-Auth Features

- [ ] TODO [GROWTH] [M] REST API for Personal+ tier: JSON endpoints for thread/message/reply; Zapier/Make integrations. HIGH impact.
- [ ] TODO [GROWTH] [M] Full-text search across threads (PostgreSQL tsvector) — Personal/Team upgrade gate. Prerequisite: basic thread search (LIKE fallback).
- [ ] TODO [GROWTH] [M] Slack/Discord webhook integration: POST to Slack when new email arrives in a thread; Team plan gate. HIGH impact. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [GROWTH] [M] Email forwarding address: assign each user a unique @mailaim.app address; email forwarded there auto-imported. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] Thread export (PDF/HTML): clean printable file; Personal/Team plan gate. MEDIUM impact.
- [ ] TODO [GROWTH] [M] Send-later scheduling for replies: schedule reply at future date/time; Personal/Team plan gate. HIGH impact.

---

## Done (archived)

- [x] DONE [CORE] Rewrite README.md into proper README
- [x] DONE [CORE] [L] Scaffold Maven project: pom.xml, mvnw, application.yml, EmailMessengerApplication.java
- [x] DONE [CORE] [M] Add all Spring Boot starters: web, thymeleaf, data-jpa, validation, mail, flyway, postgresql, h2, testcontainers
- [x] DONE [CORE] [M] Add Flyway migration V1__init.sql: EmailThread, Message, Participant, Attachment, MessageRecipient tables with indexes
- [x] DONE [CORE] [M] Implement domain entities (EmailThread, Message, Participant, MessageRecipient, Attachment) and Spring Data repositories
- [x] DONE [CORE] [L] Email-import service: parse RFC 822 via Jakarta Mail, build threads from Message-ID / In-Reply-To / References
- [x] DONE [CORE] [M] IM transform: IMTransformService (stripQuotes + renderMarkdown), ConversationService (BubbleRun grouping), Conversation/BubbleRun/BubbleMessage view model records
- [x] DONE [HEALTH] [M] Sanitize HTML email bodies: jsoup 1.17.2 added; ConversationService.buildBodyHtml calls Jsoup.clean(bodyHtml, Safelist.relaxed()) — closes CRITICAL XSS vector
- [x] DONE [UX] [S] Participant initials utility: added initials() method to Participant entity
- [x] DONE [HEALTH] [S] EmailImportService: wrap MessagingException and IOException in EmailImportException (unchecked)
- [x] DONE [HEALTH] [S] Add global exception handler: GlobalExceptionHandler handles 404/409/502/500; disables Whitelabel error page
- [x] DONE [CORE] [L] Thymeleaf templates: threads.html, conversation.html, main.css, ThreadController, ThreadViewService, ReplyService
- [x] DONE [HEALTH] [S] Add input validation for all web form objects: ReplyForm @NotBlank + @Size; ThreadController uses @Valid
- [x] DONE [UX] [S] Thread list empty state, conversation empty state, reply button primary CTA
- [x] DONE [UX] [S] Bubble body HTML rendering: th:utext with sanitization contract comment
- [x] DONE [CORE] [M] CSS for the IM look: day separators, dark mode, refined bubbles, header-nav and msg-count classes
- [x] DONE [UX] [S] Keyboard shortcuts: j/k navigate, Enter open, r reply, Esc cancel
- [x] DONE [CORE] [M] IMAP polling job: ImapPollingJob (@ConditionalOnProperty, @Scheduled), ImapPollingProperties (@ConfigurationProperties), feature flag app.imap.polling.enabled; 72 tests pass
- [x] DONE [GROWTH] [S] Static pricing page at /pricing: PricingController + pricing.html with annual/monthly toggle, plan comparison (Free/Personal/Team/Enterprise), feature matrix, FAQ section, OG/meta tags; 76 tests pass
- [x] DONE [GROWTH] [S] Demo mode: DemoService (2 realistic conversations), DemoController (GET /demo + GET /demo/{id}), demo.html with CTA; conversation.html isDemo flag; "Demo" nav link; OG/meta tags; 14 new tests; 90 tests pass
- [x] DONE [GROWTH] [S] Waitlist email capture at /waitlist: WaitlistEntry JPA entity, V2__waitlist.sql migration, WaitlistEntryRepository (existsByEmail), WaitlistForm (@Email @NotBlank), WaitlistController (GET + POST with duplicate detection), waitlist.html (form / success / already-joined states), waitlist CSS; pricing CTAs updated to /waitlist; demo and conversation CTAs updated; 99 tests pass
- [x] DONE [UX] [S] Pricing page CTAs (Personal, Team) updated from /threads to /waitlist
- [x] DONE [UX] [S] Pricing page privacy/TOS links fixed: /privacy and /terms stub pages created (LegalController), linked from pricing footer and FAQ; 99 tests pass
- [x] DONE [UX] [S] Demo conversation banner "Connect your own mailbox" updated to "Join the waitlist →" (/waitlist)
- [x] DONE [UX] [S] Threads empty state CTA updated from non-existent /settings/mailboxes to /waitlist with better copy
