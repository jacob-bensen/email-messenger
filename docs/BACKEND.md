# Backend architecture — email-messenger

Spring Boot 3.5 / Java 21 monolith. Serves two things off the same JVM:

1. **The website** — server-rendered Thymeleaf pages under
   `src/main/resources/templates/`, mounted at `/`, `/chats`, `/mailboxes`,
   `/account`, `/billing`, `/pricing`, `/admin`, etc.
2. **The mobile JSON API** — `/api/v1/**`, defined by `docs/API.md`.
   Currently just `GET /api/v1/meta`.

Both surfaces MUST go through the same service beans — no forked business
logic between a `@Controller` and a `@RestController`.

## Modules (Java packages under `com.emailmessenger`)

| Package | Role |
| --- | --- |
| `api/` | JSON REST controllers for the mobile client. New endpoints live here, not in `web/`. |
| `web/` | Thymeleaf controllers (`@Controller` returning template names), form objects, SSE stream for the website. |
| `service/` | Cross-cutting business logic: `ConversationService`, `IMTransformService`, `ReplyService`, `ConversationKeyService`, `EmailBodyCleaner`, view models (`Conversation`, `BubbleRun`, `ChatConversation`…). |
| `domain/` | JPA `@Entity` classes + enums (`Plan`, `AuthType`, `PlanLimitKind`…). |
| `repository/` | Spring Data JPA interfaces. |
| `email/` | Mailbox connection: `ImapClient`, `JakartaImapClient`, `ImapIdleService`, `MailboxPollingService`, `MimeMessageParser`, `CredentialEncryptor`, `MailAccountService`, `NewMailEvent`. |
| `auth/` | Spring Security config, form login, Google OIDC, password reset, email verification, `AccountService`, `UserService`. |
| `billing/` | Stripe integration: `BillingService`, `StripeCheckoutGateway`, `PlanLimitService`, webhook handler. |
| `team/` | Teams + invites. |
| `admin/` | `/admin/**` operator surface + weekly metrics digest. |
| `activation/`, `reengagement/`, `digest/` | Scheduled lifecycle email jobs (each behind an `*.enabled` flag). |

## Persistence

- **PostgreSQL 16** in prod and local (`localhost:5432/email_messenger`,
  bootstrap via `src/main/resources/db/init.sql`).
- **H2** in tests via `src/test/resources/application.yml`.
- **Flyway** migrations under `src/main/resources/db/migration/` — currently
  V1..V35. Immutable once merged; every schema change is a new
  `V{n}__*.sql` (no jOOQ codegen — the routine's generic stack description
  mentions jOOQ, but this project uses Spring Data JPA with
  `hibernate.ddl-auto: validate` against the Flyway-managed schema).

## Auth model

- Website: form login (`POST /login`, session cookie) + optional Google
  OIDC (`spring-boot-starter-oauth2-client`, activated when
  `auth.google.client-id` and `client-secret` are both non-blank).
- Remember-me: JDBC-backed persistent tokens (30 days).
- Login throttling: `LoginThrottleFilter` runs before the form-login
  filter; per-email lockout on repeated failures.
- Mobile: **not yet implemented.** Session-based mobile auth is the next
  API-contract item.

## Live delivery

- **IMAP IDLE** per paid-tier mailbox: `ImapIdleService` +
  `JakartaImapIdleClient`, gated by `mailbox.idle.enabled`.
- **Polling** for free tier: `MailboxPollingScheduler` calls
  `MailboxPollingService` on `mailbox.polling.interval-ms` cadence.
- **SSE** for the website: `/chats/events` (`ChatEventStream` +
  `ChatEventsController`). `MailboxPollingService` publishes
  `NewMailEvent`s; the stream forwards them to per-user tabs.

## Billing

Stripe Checkout for upgrades; Stripe Customer Portal for cancel/change;
webhook at `/billing/webhook` (CSRF-exempt, signature-verified). Plans
mirror `com.emailmessenger.domain.Plan`: `FREE`, `PRO`, `BUSINESS`.
`PlanLimitService` enforces per-plan caps (mailbox count, attachment
storage, etc.) using `PlanLimitKind`.

## Website → API relationship

Every Thymeleaf controller under `web/` and every JSON controller under
`api/` reaches the database through the same `service/`, `repository/`,
`email/`, `billing/`, `auth/`, and `team/` beans. Adding a mobile
endpoint that duplicates a website flow's logic in the controller
instead of extending the shared service is a review-block: keep
business logic in `service/` (or the appropriate topic package) and
have both surfaces call it.

## Build & test

- `./mvnw clean package` — builds the jar and emits
  `META-INF/build-info.properties` (used by `/api/v1/meta`).
- `./mvnw test` — 795 tests as of this doc's creation; JUnit 5 + Spring
  Boot Test + Testcontainers (Postgres) + GreenMail (SMTP/IMAP).
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` — dev server
  on `:8080` against local Postgres.

## What's authoritative here

The backend is the source of truth for:

- All persistent state (users, mail accounts, threads, messages,
  subscriptions, teams, saved searches, digest preferences).
- All business rules (threading, quoted-reply stripping, IM transform,
  plan-limit checks, billing state transitions, invite acceptance).
- All outbound mail (SMTP via `OutboundMessageService` / `ReplyService`).
- All timestamps (`Clock` bean injected everywhere — tests use a fixed
  clock).

The mobile client mirrors these rules in `lib/domain/` for optimistic
prediction only; on conflict, the server wins.
