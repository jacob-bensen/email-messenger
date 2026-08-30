# Backend architecture (ConexusMail server)

Spring Boot 3 / Java 21 / PostgreSQL 16 / JPA + Hibernate / Flyway / Maven.

The backend serves BOTH the ConexusMail website (Thymeleaf, server-rendered)
AND, going forward, a REST API consumed by the `conexus-mail-mobile` Flutter
client. Both must share the same service layer — no business logic is
duplicated in controllers or templates.

Note the target stack of the generic full-stack routine assumed jOOQ; this
codebase in fact uses JPA. Keep it that way.

## Module layout

```
com.emailmessenger
├── EmailMessengerApplication      Spring Boot entrypoint
├── activation                     onboarding / activation nudges
├── admin                          admin metrics + revenue view
├── auth                           form login + Google OIDC, throttling, verification, password reset
├── billing                        Stripe integration (checkout, webhooks, plan changes)
├── digest                         digest email opt-out + scheduling
├── domain                         JPA entities (User, MailAccount, EmailThread, Message, …)
├── email                          IMAP polling + IDLE, SMTP send, RFC 822 parsing
├── reengagement                   dormant-user email jobs
├── repository                     Spring Data repositories
├── service                        conversation building, IM transform, reply, chat-list
├── team                           team invites, membership
└── web                            Thymeleaf controllers, view models, form objects, SSE
```

## Persistence

- PostgreSQL 16 in prod/local; H2 in tests (`src/test/resources/application.yml`).
- `application.yml` reads `JDBC_DATABASE_URL` / `_USERNAME` / `_PASSWORD` env
  vars. `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`. `db/init.sql` bootstraps db + app user.
- Migrations under `src/main/resources/db/migration/V{n}__*.sql`.
  Immutable once merged — always add a new file rather than editing one.
- Hibernate is set to `validate`; Flyway owns schema evolution.

Key entities (in `com.emailmessenger.domain`):

- `User` — the account holder. Owns `MailAccount`s, `Subscription`, teams.
- `MailAccount` — one connected mailbox (IMAP+password or Gmail OAuth). Holds
  polling cursor + last-seen sent UID.
- `EmailThread` — grouping of messages by RFC-822 threading headers, joined
  onto a `conversationKey` (participant-set hash) for the /chats view.
- `Message` — one email (headers, plain + html body, sent-at, direction).
- `Participant`, `MessageRecipient`, `Attachment` — supporting rows.
- `Subscription`, `Plan`, `PlanChangeEvent` — billing.
- `Team`, `TeamMember`, `TeamInvite` — multi-user accounts.
- `SavedSearch`, `DigestEmailPreference`, `PasswordResetToken`,
  `EmailVerificationToken`, `AuthEvent` — supporting features.

## Auth model

- Form login (email + BCrypt password) via `AuthController` +
  `DbUserDetailsService`.
- Google OIDC via `spring-boot-starter-oauth2-client`, gated on
  `GOOGLE_CLIENT_ID` env var — see `SecurityConfig.securityFilterChain`.
- Remember-me tokens persist in `JdbcTokenRepositoryImpl`.
- Login throttling via `LoginThrottleFilter` (runs inside the Spring Security
  chain, auto-registration disabled).
- `SecurityConfig` currently authorizes the whole surface as either
  `permitAll()` (public web pages, static assets, /actuator/health) or
  `authenticated()` (everything else). The mobile API path (`/api/**`)
  needs its own auth story — see INTERNAL_TODO.md.

## Web ↔ API relationship

The website (`web/*Controller.java` returning Thymeleaf views) and the
future REST API MUST both call the same services in `service/*`,
`auth/*Service.java`, `email/*Service.java`, and `billing/*Service.java`.
Controllers exist only to translate HTTP↔service DTOs and select a view.

See `docs/API.md` for the REST contract and `docs/WEB.md` for the page
inventory.

## Background jobs

- `MailboxPollingService` — `@Scheduled` IMAP polling per mail account.
- `ImapIdleService` — long-lived IMAP IDLE for paid-tier accounts, flagged
  by `mailbox.idle.enabled`.
- Digest / reengagement / activation nudge services — `@Scheduled` cadence
  driven from persisted per-user timestamps.

## Live delivery

- `ChatEventStream` publishes `NewMailEvent`s to browser clients via
  Server-Sent Events at `/chats/events`.
- The chat page morphs the rail + open pane in place on event receipt
  (`chat.html`, `fragments/live-refresh.html`).
- Mobile clients will need an equivalent — either an SSE endpoint under
  `/api/v1/events` or (preferred for battery) FCM/APNs push. Design not yet
  chosen.

## Build

- `./mvnw clean package` — build
- `./mvnw test` — run tests (H2 in-memory)
- `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
  — run locally against PostgreSQL
