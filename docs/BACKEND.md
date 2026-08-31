# BACKEND — email-messenger

Spring Boot 3 app that both:
1. Serves the public web product (`ConexusMail`) via Thymeleaf server-rendered pages.
2. Owns all persistence and business logic. A mobile REST API is **planned but not
   yet implemented** — see `docs/API.md`.

## Stack

- Java 21, Spring Boot 3.x
- Spring MVC (`spring-boot-starter-web`), Thymeleaf, Spring Security (form + Google
  OIDC), Spring Data JPA, Bean Validation, Spring Mail
- PostgreSQL 16 in dev/prod, H2 in tests
- Flyway for schema migrations (`src/main/resources/db/migration/V*.sql`, immutable
  once merged — add a new `V{n}__*.sql` rather than editing an existing one)
- Maven with wrapper (`./mvnw`)
- Test stack: JUnit 5, Spring Boot Test, Testcontainers, GreenMail (SMTP/IMAP)

> Note: The routine prompt anticipates jOOQ, but this repo uses **JPA + Flyway**, not
> jOOQ. Treat that as the source of truth. There is no code-gen step.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   # @SpringBootApplication entry point
├── auth/                            # login/register/OIDC, account settings,
│                                    #   password reset, email verification,
│                                    #   throttling, audit
├── billing/                         # Stripe checkout/portal, webhook, cancellation
├── domain/                          # JPA entities (User, MailAccount, EmailThread,
│                                    #   Message, Participant, Attachment, Team,
│                                    #   Subscription, ...)
├── repository/                      # Spring Data repositories
├── service/                         # Import parsing, IM transform, conversation
│                                    #   grouping (conversationKey), reply, cleaner
├── email/                           # IMAP integration, polling job, IDLE service,
│                                    #   outbound SMTP
├── web/                             # Thymeleaf controllers + form/view objects,
│                                    #   SSE stream (/chats/events),
│                                    #   PWA manifest & service worker
├── team/                            # Team invites and membership
├── digest/                          # Weekly digest emails + opt-out
├── admin/                           # /admin metrics + revenue tools
├── activation/                      # Onboarding nudges (day-1, day-3, last-chance)
└── reengagement/                    # Idle-user emails
```

Templates live in `src/main/resources/templates/`, static assets in
`src/main/resources/static/`, migrations in `src/main/resources/db/migration/`.

## Auth model

- Session-based via Spring Security. Two authentication paths:
  - Form login (`/login`) against the local `users` table (BCrypt).
  - Google OIDC (`/auth/google/start` → `/login/oauth2/code/google`).
- `DbUserDetailsService` loads the `User`. `SecurityConfig` wires the filter chain,
  including `LoginThrottleFilter`.
- All authenticated endpoints receive a `java.security.Principal` whose `getName()`
  is the user's email; controllers resolve `User` via `UserService.requireByEmail`.
- **No token-based auth exists today.** A mobile REST API will need either
  session-with-CSRF (awkward for native clients) or an added token/OAuth layer.
  Recorded in `INTERNAL_TODO.md`.

## Persistence & schema highlights

- Users, subscriptions (Free/Pro/Business), and plan history live in `users`,
  `subscriptions`, `plan_change_events`.
- Mailbox connections in `mail_accounts` — IMAP-password OR Gmail-OAuth
  (`auth_type`), with per-account polling policy, last-seen UIDs (inbox + sent), and
  optional signature.
- Mail data in `email_threads` (grouped by `conversation_key` since V32),
  `messages`, `message_recipients`, `participants`, `attachments`
  (`blob_ref` is still null — attachments aren't downloadable yet).
- Teams and invites in `teams`, `team_members`, `team_invites`.
- Auth activity in `auth_events`, `password_reset_tokens`,
  `email_verifications`.

Latest migration at time of writing: `V35__mail_account_signature.sql`.

## Email ingest & live delivery

- `MailboxPollingService` (`@Scheduled`) polls each connected mailbox for new inbox
  and sent messages, threading them by `Message-ID` / `In-Reply-To` / `References`.
- `ImapIdleService` holds IDLE connections for paid-tier accounts
  (flag: `mailbox.idle.enabled`). New messages publish `NewMailEvent`.
- `ChatEventStream` fans events out to the browser over
  SSE (`/chats/events`) so open chat panes morph in place.
- Outbound send goes through `OutboundMessageService` (SMTP) and, on success,
  records a `messages` row with `outbound = true`.

## IM transform

- `EmailBodyCleaner` strips quoted replies (`> …`, "On … wrote:"), signatures, and
  auto-footers.
- `IMTransformService` groups consecutive same-sender messages into
  `BubbleRun`/`SenderBubbleRun` and produces `ChatConversation` view models keyed
  by `conversationKey` (participant-set hash from `ConversationKeyService`).

## Running locally

```
JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Postgres is bootstrapped via `db/init.sql` (creates `email_messenger` DB + app
user). Tests use H2 automatically from `src/test/resources/application.yml`.

## Related repositories

- **Mobile client:** [`jacob-bensen/conexus-mail-mobile`](https://github.com/jacob-bensen/conexus-mail-mobile)
  — Flutter app that will mirror the same domain rules against a (future) REST API.

## Docs in this folder

- `docs/API.md` — canonical API contract. Currently inventories the HTML/web
  endpoints and lists the mobile REST endpoints as TBD.
- `docs/WEB.md` — Thymeleaf page inventory, routes, and their controllers/services.
