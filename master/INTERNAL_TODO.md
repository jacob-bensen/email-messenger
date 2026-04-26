# Internal TODO

Format: [STATUS] [TAG] [SIZE] Description

Statuses: TODO, IN-PROGRESS, DONE, BLOCKED
Tags: CORE, GROWTH, HEALTH, TEST-FAILURE, UX
Sizes: S=< 2h, M=2-4h, L=4-8h

---

## Active / High Priority

### Core Features (income-blocking)

- [ ] TODO [CORE] [M] IMAP polling job (@Scheduled) behind a feature flag

### Growth / Revenue

- [ ] TODO [GROWTH] [M] User registration and authentication (Spring Security + email/password + remember-me) — prerequisite for billing and multi-tenancy
- [ ] TODO [GROWTH] [M] Add Stripe billing integration: subscription plans (Free/Personal/Team), checkout flow, webhook handler for subscription events
- [ ] TODO [GROWTH] [S] Add Stripe customer portal integration: self-service plan upgrade/downgrade/cancellation via Stripe Billing Portal; reduces churn from users who can't self-serve. HIGH income impact. Prerequisite: Stripe billing task.
- [ ] TODO [GROWTH] [S] Add plan-limit enforcement: max mailboxes per plan, max thread history, show upgrade prompt at limit
- [ ] TODO [GROWTH] [S] Upgrade prompt inline in thread list when user reaches free tier limit (500 threads or 1 mailbox); show modal with plan comparison table (HIGH income impact)
- [ ] TODO [GROWTH] [S] In-app upgrade preview of locked features: show disabled/blurred Team-tier features (Slack webhook, full-text search) with "Upgrade to unlock" CTA; direct conversion trigger. HIGH income impact.
- [ ] TODO [GROWTH] [S] Demo mode: pre-seed 2–3 realistic sample threads visible at /demo without requiring an account; lets landing-page visitors experience the IM view before signing up. HIGH income impact (removes top-of-funnel uncertainty).
- [ ] TODO [GROWTH] [M] SEO-friendly static landing page at / with features, pricing table, and CTA; serves organic traffic before users register
- [ ] TODO [GROWTH] [M] First-run onboarding wizard: guided "connect your first mailbox" flow with progress steps; reduces signup-to-value drop-off (HIGH income impact)
- [ ] TODO [GROWTH] [S] Add Google OAuth single sign-on: lower signup friction and auto-populate Gmail mailbox connection (HIGH income impact)
- [ ] TODO [GROWTH] [S] Annual/monthly billing toggle on pricing/settings page: show monthly vs annual pricing with "Save 16%" label; higher LTV per conversion. HIGH income impact. Prerequisite: Stripe billing task.
- [ ] TODO [GROWTH] [M] REST API for Personal+ tier: JSON API for thread/message/reply operations; enables Zapier/Make integrations; key upsell feature gate for developers. HIGH income impact.
- [ ] TODO [GROWTH] [M] Add Gravatar + initials avatar fallback for Participant display (initials() helper already added to Participant entity)
- [ ] TODO [GROWTH] [S] Add unread thread tracking: mark-as-read on view, unread count badge in thread list (engagement driver)
- [ ] TODO [GROWTH] [M] Add full-text search across threads (PostgreSQL tsvector) — key feature gate for Personal/Team upgrade
- [ ] TODO [GROWTH] [M] Thread permalink sharing: generate a shareable read-only link to a thread view (e.g. /share/{token}); low-friction demo of app value = viral touchpoint. HIGH income impact.
- [ ] TODO [GROWTH] [M] Slack/Discord webhook integration: POST a message to a configured Slack channel when a new email arrives in a thread — team plan ($29/mo) feature gate. HIGH income impact for team conversion.
- [ ] TODO [GROWTH] [M] Email forwarding address: assign each user a unique @mailaim.app address; any email forwarded there is auto-imported — completely avoids IMAP credential setup. HIGH income impact (zero-friction onboarding path).
- [ ] TODO [GROWTH] [S] Add shareable public referral link for "Invite a teammate" — awards 1 month free on conversion
- [ ] TODO [GROWTH] [M] Add email digest notifications (daily/weekly summary of unread threads) — re-engagement driver; reduces churn
- [ ] TODO [GROWTH] [M] Thread export (PDF/HTML): export a full thread as a clean printable file — useful for freelancers and support teams; Personal/Team plan gate. MEDIUM income impact.
- [ ] TODO [GROWTH] [M] Thread labels/tags: user-defined labels assignable to threads (e.g. "Client", "Urgent"); Team plan feature gate. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] EML file upload: allow users to upload a raw .eml file to seed threads instantly; useful for demos, one-off imports, and offline testing. MEDIUM income impact.
- [ ] TODO [GROWTH] [M] Send-later scheduling for replies: schedule a reply to be sent at a future date/time; useful for async remote workers across time zones; Personal/Team plan gate. HIGH income impact.
- [ ] TODO [GROWTH] [S] Thread snooze: snooze a thread to re-surface at a set time; reduces inbox anxiety and drives daily re-engagement. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] Browser push notifications via Web Push API: notify users in-browser when a new email arrives in a watched thread — drives daily active usage. MEDIUM income impact. Requires service worker.
- [ ] TODO [GROWTH] [S] "Sent via MailIM" branding footer in outgoing replies for Free plan users: each email sent becomes a distribution touchpoint; disabled for Personal+ plan. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] Public roadmap page at /roadmap: static HTML listing upcoming features; reduces "is this abandoned?" churn; generates shareability. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] In-app referral prompt: after user imports 10+ threads show "Loving MailIM? Invite a colleague" modal with pre-filled tweet text and copy-to-clipboard referral link. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] Custom SMTP/from-address settings per user account: allow users to configure their own "From" email address and display name for outgoing replies (currently defaults to spring.mail.username); critical for replies to appear as the user's own email identity in production. HIGH income impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [M] AI-generated thread summary: one-sentence summary per thread shown in the thread list; powered by Claude API; Personal+ tier gate; differentiates MailIM from standard email clients. HIGH income impact (unique feature). Prerequisite: user auth + Anthropic API key config.
- [ ] TODO [GROWTH] [S] Reply signature: per-user configurable HTML/text signature appended to all outgoing replies; increases reply adoption and product stickiness. MEDIUM income impact. Prerequisite: user auth.
- [ ] TODO [GROWTH] [S] Outbound webhook trigger on new message: allow users to POST to a configured URL when a new thread message arrives (simplified Zapier/Make integration); Team plan gate. MEDIUM income impact. Prerequisite: IMAP polling.
- [ ] TODO [GROWTH] [S] "Copy conversation as Markdown" button: one-click copy of full thread as clean Markdown to clipboard; useful for Notion/Slack/docs; zero-friction share touchpoint. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] Static pricing page at /pricing: plan comparison table (Free/Personal/Team/Enterprise) with feature matrix and CTA buttons; no auth required; prerequisite for organic conversion from landing page. HIGH income impact.
- [ ] TODO [GROWTH] [S] 14-day free trial on Personal tier: set trial_period_days=14 on the Stripe Personal plan price; no credit card required on signup; highest-leverage SaaS conversion mechanism. HIGH income impact. Prerequisite: Stripe billing.
- [ ] TODO [GROWTH] [S] PWA web app manifest: add manifest.json + apple-touch-icon to enable "Add to Home Screen" on iOS/Android; zero-cost mobile distribution channel; users who install the PWA have 3× higher 30-day retention. MEDIUM income impact.
- [ ] TODO [GROWTH] [S] Thread archiving: "Archive" action on each thread; archived threads hidden from main list; /archived route to view them; essential GTD workflow for power users driving Personal plan upgrade. MEDIUM income impact. Prerequisite: user auth (per-user archive state).
- [ ] TODO [GROWTH] [S] Conversation pinning: pin up to 3 threads to the top of the thread list; per-user state; drives team plan upgrade for "pin important client threads" use case. MEDIUM income impact. Prerequisite: user auth.

### UX

- [ ] TODO [UX] [S] Thread list: show last-message-body preview (first 80 chars) below the subject line — requires adding a last_message_preview column to email_threads or denormalizing via query; current UI shows only subject + count + date
- [ ] TODO [UX] [S] Thread list header navigation: the "+ Add mailbox" link currently points to /settings/mailboxes which does not yet exist; implement the mailbox settings page (or redirect to auth/onboarding once auth ships) so the link is not a dead-end
- [ ] TODO [UX] [S] IMAP sync status indicator: show "last synced X minutes ago" in the thread list header so users know when their data is fresh; update via polling or SSE
- [ ] TODO [UX] [M] Mobile layout pass: ensure thread list and conversation bubble view are usable on 375px-wide screens; bubbles should not overflow viewport; basic responsive CSS exists, full pass needed

### Infrastructure

- [ ] TODO [CORE] [L] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP)
- [ ] TODO [CORE] [M] Dockerfile + docker-compose.yml (app + postgres)
- [ ] TODO [CORE] [S] GitHub Actions CI: build, test, cache Maven deps

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
- [x] DONE [HEALTH] [S] EmailImportService: wrap MessagingException and IOException in EmailImportException (unchecked) — importMessage() no longer leaks mail-stack checked exceptions across layer boundaries
- [x] DONE [HEALTH] [S] Add global exception handler: GlobalExceptionHandler (@ControllerAdvice) with error.html template; handles NoResourceFoundException (404), NoSuchElementException (404), MailException/EmailImportException (502), DataIntegrityViolationException (409), and generic Exception (500); disables Whitelabel error page
- [x] DONE [CORE] [L] Thymeleaf templates: threads.html (thread list with empty state + pagination), conversation.html (chat bubbles + reply form), main.css (IM look with avatars, bubbles, mobile responsive), ThreadController, ThreadViewService, ReplyService
- [x] DONE [HEALTH] [S] Add input validation for all web form objects: ReplyForm has @NotBlank + @Size(max=100,000); ThreadController uses @Valid + BindingResult; validation errors shown inline
- [x] DONE [UX] [S] Thread list empty state: threads.html shows "No conversations yet" card with CTA when threads.totalElements == 0
- [x] DONE [UX] [S] Conversation view empty state: conversation.html shows "No messages in this thread yet" when runs list is empty
- [x] DONE [UX] [S] Conversation view reply button: btn-primary styled blue button is the visually dominant action in the reply-area
- [x] DONE [UX] [S] Bubble body HTML rendering: conversation.html uses th:utext with a warning comment explaining the sanitization contract
- [x] DONE [CORE] [M] CSS for the IM look: day separators (JS-inserted, date-aware with Today/Yesterday labels), dark mode (@media prefers-color-scheme), refined bubble borders + hover shadow, header-nav and msg-count CSS classes replacing inline styles
- [x] DONE [UX] [S] Keyboard shortcuts: j/k to navigate thread list with .kb-focus indicator, Enter to open, r to focus reply textarea, Esc to blur
