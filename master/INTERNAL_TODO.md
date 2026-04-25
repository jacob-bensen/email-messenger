# Internal TODO

Format: [STATUS] [TAG] [SIZE] Description

Statuses: TODO, IN-PROGRESS, DONE, BLOCKED
Tags: CORE, GROWTH, HEALTH, TEST-FAILURE
Sizes: S=< 2h, M=2-4h, L=4-8h

---

## Active / High Priority

- [ ] TODO [CORE] [M] Add Flyway and write V1__init.sql for EmailThread, Message, Participant, Attachment tables
- [ ] TODO [CORE] [M] Implement domain entities (EmailThread, Message, Participant, Attachment) and Spring Data repositories
- [ ] TODO [CORE] [L] Email-import service: parse RFC 822 via Jakarta Mail, build threads from Message-ID / In-Reply-To / References
- [ ] TODO [CORE] [M] IM transform: strip quoted replies (> ..., "On … wrote:"), collapse consecutive same-sender messages, render basic markdown
- [ ] TODO [CORE] [L] Thymeleaf templates: thread list, conversation view with chat bubbles, reply form
- [ ] TODO [CORE] [M] CSS for the IM look: avatars, bubbles, day separators, dark mode
- [ ] TODO [CORE] [M] IMAP polling job (@Scheduled) behind a feature flag
- [ ] TODO [CORE] [L] Integration tests with Testcontainers (Postgres) + GreenMail (SMTP/IMAP)
- [ ] TODO [CORE] [M] Dockerfile + docker-compose.yml (app + postgres)
- [ ] TODO [CORE] [S] GitHub Actions CI: build, test, cache Maven deps

## Growth

- [ ] TODO [GROWTH] [M] Add Stripe billing integration: subscription plans (Free/Personal/Team), checkout flow, webhook handler for subscription events
- [ ] TODO [GROWTH] [M] User registration and authentication (Spring Security + email/password + remember-me)
- [ ] TODO [GROWTH] [S] Add plan-limit enforcement: max mailboxes per plan, max thread history
- [ ] TODO [GROWTH] [M] Add Gravatar + initials avatar fallback for Participant display
- [ ] TODO [GROWTH] [S] Add unread thread tracking: mark-as-read on view, unread count badge in thread list
- [ ] TODO [GROWTH] [M] Add full-text search across threads (PostgreSQL tsvector or Meilisearch)
- [ ] TODO [GROWTH] [S] Add shareable public referral link for "Invite a teammate" — awards 1 month free on conversion

## Health

- [ ] TODO [HEALTH] [S] Add input validation for all web form objects using jakarta.validation
- [ ] TODO [HEALTH] [S] Add global exception handler (@ControllerAdvice) with user-friendly error pages

## Done (archived)

- [x] DONE [CORE] Rewrite README.md into proper README
- [x] DONE [CORE] [L] Scaffold Maven project: pom.xml, mvnw, application.yml, EmailMessengerApplication.java
- [x] DONE [CORE] [M] Add all Spring Boot starters: web, thymeleaf, data-jpa, validation, mail, flyway, postgresql, h2, testcontainers
