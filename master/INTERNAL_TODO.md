# Internal TODO

Format: [STATUS] [TAG] [SIZE] Description

Statuses: TODO, IN-PROGRESS, DONE, BLOCKED
Tags: CORE, GROWTH, HEALTH, TEST-FAILURE, UX
Sizes: S=< 2h, M=2-4h, L=4-8h

---

## Active / High Priority

### Core Features (income-blocking)

- [ ] TODO [CORE] [L] Email-import service: parse RFC 822 via Jakarta Mail, build threads from Message-ID / In-Reply-To / References
- [ ] TODO [CORE] [M] IM transform: strip quoted replies (> ..., "On … wrote:"), collapse consecutive same-sender messages, render basic markdown
- [ ] TODO [CORE] [L] Thymeleaf templates: thread list, conversation view with chat bubbles, reply form
- [ ] TODO [CORE] [M] CSS for the IM look: avatars, bubbles, day separators, dark mode
- [ ] TODO [CORE] [M] IMAP polling job (@Scheduled) behind a feature flag

### Growth / Revenue

- [ ] TODO [GROWTH] [M] User registration and authentication (Spring Security + email/password + remember-me) — prerequisite for billing and multi-tenancy
- [ ] TODO [GROWTH] [M] Add Stripe billing integration: subscription plans (Free/Personal/Team), checkout flow, webhook handler for subscription events
- [ ] TODO [GROWTH] [S] Add plan-limit enforcement: max mailboxes per plan, max thread history, show upgrade prompt at limit
- [ ] TODO [GROWTH] [M] First-run onboarding wizard: guided "connect your first mailbox" flow with progress steps; reduces signup-to-value drop-off (HIGH income impact)
- [ ] TODO [GROWTH] [S] Add Google OAuth single sign-on: lower signup friction and auto-populate Gmail mailbox connection (HIGH income impact)
- [ ] TODO [GROWTH] [M] Add Gravatar + initials avatar fallback for Participant display
- [ ] TODO [GROWTH] [S] Add unread thread tracking: mark-as-read on view, unread count badge in thread list (engagement driver)
- [ ] TODO [GROWTH] [M] Add full-text search across threads (PostgreSQL tsvector) — key feature gate for Personal/Team upgrade
- [ ] TODO [GROWTH] [S] Add shareable public referral link for "Invite a teammate" — awards 1 month free on conversion
- [ ] TODO [GROWTH] [M] Add email digest notifications (daily/weekly summary of unread threads) — re-engagement driver; reduces churn
- [ ] TODO [GROWTH] [S] Upgrade prompt inline in thread list when user reaches free tier limit (500 threads or 1 mailbox); show modal with plan comparison table (HIGH income impact)
- [ ] TODO [GROWTH] [M] SEO-friendly static landing page at / with features, pricing table, and CTA; serves organic traffic before users register

### UX

- [ ] TODO [UX] [S] Thread list empty state: when no threads exist show "Connect your first mailbox" card with a prominent CTA button — currently users see a blank page with no direction
- [ ] TODO [UX] [S] Conversation view empty state: when a thread has no messages show explanatory copy, not a blank panel
- [ ] TODO [UX] [S] Error pages: replace default Spring Whitelabel error page with user-friendly error.html template (plain-English message, link back to home)
- [ ] TODO [UX] [S] Conversation view reply button: the "Reply" affordance must be the visually dominant primary action (large, colored button); do not bury it below the message list
- [ ] TODO [UX] [M] Mobile layout pass: ensure thread list and conversation bubble view are usable on 375px-wide screens; bubbles should not overflow viewport

### Health

- [ ] TODO [HEALTH] [S] Add input validation for all web form objects using jakarta.validation (@NotBlank, @Email, @Size)
- [ ] TODO [HEALTH] [S] Add global exception handler (@ControllerAdvice) with user-friendly error pages for common exceptions (NotFoundException, MailException, DataIntegrityViolationException)

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
- [x] DONE [CORE] [M] Implement domain entities (EmailThread, Message, Participant, MessageRecipient, Attachment) and Spring Data repositories (ParticipantRepository, EmailThreadRepository, MessageRepository, AttachmentRepository)
