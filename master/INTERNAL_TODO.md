# Internal TODO

Format: [STATUS] [TAG] [SIZE] Description

Statuses: TODO, IN-PROGRESS, DONE, BLOCKED
Tags: CORE, GROWTH, HEALTH, TEST-FAILURE, UX
Sizes: S=< 2h, M=2-4h, L=4-8h

---

## Active / High Priority

### Health

- [ ] TODO [HEALTH] [S] Add CSRF protection when Spring Security ships: Spring Security's CSRF filter should be enabled (default) to prevent cross-site form submission attacks on the reply endpoint. No action needed until user auth task starts.
- [ ] TODO [HEALTH] [S] Rate-limit the reply POST endpoint to prevent form abuse (e.g. Spring's Bucket4j or a simple @RateLimiter via AOP); add when auth ships so the limit is per user.
- [ ] TODO [HEALTH] [S] Attachment N+1 query: Message.attachments is still loaded lazily per message in the conversation view; add @BatchSize(size=50) to Message.attachments to reduce per-message attachment queries to batches of 50. Low priority until threads with many attachments are common.

### Infrastructure (prerequisite for Dockerfile/CI)

- [ ] TODO [CORE] [L] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP)
- [ ] TODO [CORE] [M] Dockerfile + docker-compose.yml (app + postgres)
- [ ] TODO [CORE] [S] GitHub Actions CI: build, test, cache Maven deps

### No-Prerequisite Growth (ship next, highest ROI)

- [x] DONE [GROWTH] [S] Demo mode: pre-seed 2–3 realistic sample threads visible at /demo without an account; lets visitors experience the IM view before signing up. HIGH income impact.
- [ ] TODO [GROWTH] [S] Demo page SEO: add keyword-rich h2 sub-heading, feature bullet list, and JSON-LD SoftwareApplication schema to /demo; rank for "email as chat app" searches. MEDIUM impact. Prerequisite: demo page (done ✓).
- [ ] TODO [GROWTH] [S] Robots.txt endpoint: Spring @Controller returning /robots.txt; Allow: /demo, /pricing, /; Disallow: /threads, /settings; submit sitemap.xml link. LOW impact, no prerequisites.
- [ ] TODO [GROWTH] [S] Waitlist email capture page at /waitlist: one-field form (email) backed by a WaitlistEntry JPA entity; lead gen before auth ships. HIGH income impact, no prerequisites.
- [ ] TODO [GROWTH] [M] SEO-friendly static landing page at / with features, pricing table, and CTA; serves organic traffic before users register. HIGH income impact.
- [ ] TODO [GROWTH] [S] Basic thread search by subject/sender at GET /threads?q=: JPA LIKE query on email_threads.subject and participants.email; unblocks search use case today. MEDIUM impact, no prerequisites.
- [ ] TODO [GROWTH] [M] Add Gravatar + initials avatar fallback for Participant display (initials() helper already added to Participant entity). MEDIUM impact.
- [ ] TODO [GROWTH] [M] Thread permalink sharing: generate a shareable read-only link to a thread view (e.g. /share/{token}); viral touchpoint. HIGH income impact.
- [ ] TODO [GROWTH] [S] EML file upload: upload a raw .eml file to seed threads instantly; useful for demos, one-off imports, and offline testing. MEDIUM impact.
- [ ] TODO [GROWTH] [M] .mbox file import: upload a raw .mbox archive (Google Takeout / Thunderbird export) to import all threads in bulk; removes IMAP credential requirement. HIGH impact, no prerequisites.
- [ ] TODO [GROWTH] [S] PWA web app manifest: add manifest.json + apple-touch-icon; users who install PWA have 3× higher 30-day retention. MEDIUM impact.
- [ ] TODO [GROWTH] [S] Public roadmap page at /roadmap: static HTML listing upcoming features; reduces "is this abandoned?" churn. MEDIUM impact.
- [ ] TODO [GROWTH] [S] "Copy conversation as Markdown" button: one-click copy of full thread as Markdown to clipboard. MEDIUM impact.
- [ ] TODO [GROWTH] [M] SSE live conversation refresh: Spring SseEmitter pushes "new-message" event to the open conversation page when ImapPollingJob imports new emails; makes app feel real-time. MEDIUM impact. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [GROWTH] [S] Outbound webhook trigger on new message: POST to a configured URL when a new thread message arrives (Zapier/Make integration); Team plan gate. MEDIUM impact. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [GROWTH] [S] Add unread thread tracking: mark-as-read on view, unread count badge in thread list (engagement driver). MEDIUM impact.
- [ ] TODO [GROWTH] [S] In-app upgrade preview of locked features: show disabled/blurred Team-tier features with "Upgrade to unlock" CTA. HIGH income impact.
- [ ] TODO [GROWTH] [S] Open Graph + meta description tags on all pages: add `og:title`, `og:description`, `og:type`, and `<meta name="description">` to threads.html, conversation.html, error.html; improves social-share previews every time someone posts a link. MEDIUM income impact. No prerequisites.
- [ ] TODO [GROWTH] [S] Keyboard shortcut `?` to show help modal: client-side JS modal listing all keyboard shortcuts (j/k/Enter/r/Esc); power-user delight, increases retention. LOW-MEDIUM impact. No prerequisites.
- [ ] TODO [GROWTH] [S] Sitemap.xml controller: Spring `@Controller` returning XML listing all public routes (`/`, `/pricing`, `/demo`, `/roadmap`); submit to Google Search Console; helps index pricing page faster. LOW impact, no prerequisites.
- [ ] TODO [GROWTH] [S] Social proof section on pricing page: "Trusted by X teams" or 2–3 short testimonials below the plan cards; placeholder copy OK until real users provide quotes; highest-leverage conversion booster on a pricing page. MEDIUM impact. No prerequisites for placeholder version.
- [ ] TODO [GROWTH] [S] "Sent via MailIM" branding footer in outgoing replies for Free plan users; disabled for Personal+. MEDIUM impact.

### UX

- [ ] TODO [UX] [S] Thread list: show last-message-body preview (first 80 chars) below the subject line — requires adding last_message_preview to email_threads or denormalizing via query.
- [ ] TODO [UX] [S] Thread list header navigation: the "+ Add mailbox" link points to /settings/mailboxes which doesn't exist; implement mailbox settings page or redirect to auth/onboarding once auth ships.
- [ ] TODO [UX] [S] IMAP sync status indicator: show "last synced X minutes ago" in the thread list header; update via SSE. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [UX] [M] Mobile layout pass: ensure thread list and conversation view are usable on 375px screens; bubbles should not overflow viewport.
- [ ] TODO [UX] [S] Pricing page CTAs link to /threads (the app) rather than a signup/waitlist page; new visitors land in the app in an empty state with no context. Fix: update plan CTA hrefs to /waitlist once waitlist ships, or /demo once demo ships. Prerequisite: waitlist or demo page.
- [ ] TODO [UX] [S] Pricing page privacy/TOS links are href="#" placeholders — clicking them is a dead end. Fix: create /privacy and /terms static pages (stub pages sufficient until legal copy is ready). No prerequisites.

### Auth-Gated Growth (implement user auth first, then these unlock)

- [ ] TODO [GROWTH] [M] User registration and authentication (Spring Security + email/password + remember-me) — prerequisite for billing, multi-tenancy, and all auth-gated features below.
- [ ] TODO [GROWTH] [M] First-run onboarding wizard: guided "connect your first mailbox" flow; reduces signup-to-value drop-off. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Add Google OAuth single sign-on: lower signup friction, auto-populate Gmail mailbox. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Custom SMTP/from-address settings per user account: configure "From" email for outgoing replies; critical for identity in production. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] AI-generated thread summary: one-sentence summary per thread in thread list; powered by Claude API; Personal+ tier gate. HIGH impact (unique differentiator). Prerequisite: user auth + ANTHROPIC_API_KEY.
- [ ] TODO [GROWTH] [S] Reply signature: per-user configurable HTML/text signature appended to replies. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Add shareable public referral link for "Invite a teammate" — awards 1 month free on conversion. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] In-app referral prompt: after user imports 10+ threads show "Loving MailIM? Invite a colleague" modal. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] Add email digest notifications (daily/weekly summary of unread threads) — re-engagement driver. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] Thread labels/tags: user-defined labels (e.g. "Client", "Urgent"); Team plan gate. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Thread snooze: re-surface thread at a set time. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Thread archiving: "Archive" action per thread; /archived route. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Conversation pinning: pin up to 3 threads to top of list; per-user state. MEDIUM impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Browser push notifications via Web Push API: notify on new email in watched thread. MEDIUM impact. Requires service worker. Prerequisite: user auth.

### Stripe-Gated Growth (implement user auth + Stripe billing first)

- [ ] TODO [GROWTH] [M] Add Stripe billing integration: subscription plans (Free/Personal/Team), checkout flow, webhook handler. HIGH income impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Add Stripe customer portal integration: self-service upgrade/downgrade/cancellation. HIGH impact. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] Add plan-limit enforcement: max mailboxes per plan, max thread history, show upgrade prompt at limit. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] Upgrade prompt inline in thread list when user reaches free tier limit; plan comparison modal. HIGH impact. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] Annual/monthly billing toggle on pricing/settings page with "Save 16%" label. HIGH impact. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] 14-day free trial on Personal tier: trial_period_days=14, no credit card at signup. HIGH impact. Prerequisite: Stripe billing.

### Larger Post-Auth Features

- [ ] TODO [GROWTH] [M] REST API for Personal+ tier: JSON endpoints for thread/message/reply; Zapier/Make integrations. HIGH impact.
- [ ] TODO [GROWTH] [M] Add full-text search across threads (PostgreSQL tsvector) — Personal/Team upgrade gate. Prerequisite: basic thread search (for the LIKE fallback).
- [ ] TODO [GROWTH] [M] Slack/Discord webhook integration: POST to Slack when new email arrives in a thread; Team plan gate. HIGH impact. Prerequisite: IMAP polling (done ✓).
- [ ] TODO [GROWTH] [M] Email forwarding address: assign each user a unique @mailaim.app address; email forwarded there auto-imported. HIGH impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] Thread export (PDF/HTML): export thread as clean printable file; Personal/Team plan gate. MEDIUM impact.
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
- [x] DONE [GROWTH] [S] Static pricing page at /pricing: PricingController + pricing.html with annual/monthly toggle (aria-labelled), plan comparison (Free/Personal/Team/Enterprise), feature matrix, FAQ section, OG/meta-description tags; 76 tests pass
- [x] DONE [GROWTH] [S] Demo mode: DemoService (2 hardcoded realistic conversations), DemoController (GET /demo + GET /demo/{id}), demo.html with CTA section; conversation.html isDemo flag (hides reply form, shows demo banner); "Demo" nav link in threads.html + pricing.html; "Try demo" link in pricing hero and empty state; OG/meta tags on demo.html; 14 new tests; 90 tests pass
