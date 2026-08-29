# email-messenger — Backend Architecture

Overview of the Spring Boot backend for the **ConexusMail** SaaS. The
backend owns:

- the public **website** (Thymeleaf-rendered pages under
  `src/main/resources/templates/`),
- the **REST/SSE surface** for the mobile client at
  `../conexus-mail-mobile` (namespaced under `/api/mobile/v1/**`; see
  `docs/API.md`),
- all **persistence** (PostgreSQL via Spring Data JPA + Flyway),
- all **authoritative business logic** — validation, plan enforcement,
  IM transform, threading, delivery-state derivation, revenue metrics.

Website and mobile go through the same service layer. Controllers only
adapt to their transport (HTML view vs. JSON body).

## Module map

| Package | Role |
|---|---|
| `EmailMessengerApplication` | Spring Boot entry point. |
| `web/` | Thymeleaf controllers for chats, mailboxes, account, PWA shell, marketing pages; SSE `ChatEventStream` and its controller; onboarding + nav model advice. |
| `auth/` | Registration, form login, password reset, email verification, Google OIDC start callback, `AccountController` for settings. |
| `billing/` | Stripe checkout, portal, cancel, and `BillingWebhookController` (raw-body, signature-verified). |
| `service/` | Cross-cutting domain services: email import, IM transform, threading, reply, plan enforcement, notifications. |
| `email/` | IMAP + SMTP integration (`MailboxPollingService`, `ImapIdleService`, `MailSender`, quoted-reply cleanup). |
| `domain/` | JPA entities + enums (see full list below). |
| `repository/` | Spring Data JPA repositories. |
| `admin/` | Operator-only revenue metrics + retention actions; gated by `admin.emails` allowlist in `application.yml`. |
| `team/` | Team invites and acceptance. |
| `digest/` | Weekly digest email opt-out endpoint + `AdminWeeklyDigestService`. |
| `activation/` | Nudge/followup/last-chance email schedulers for new signups. |
| `reengagement/` | Win-back and reactivation email flows. |

## Domain entities

Entities live in `com.emailmessenger.domain`. As of the 2026-08-29
audit:

```
Attachment, AuthEvent, DigestEmailPreference, EmailThread,
EmailVerificationToken, MailAccount, Message, MessageRecipient,
Participant, PasswordResetToken, PlanChangeEvent, SavedSearch,
Subscription, Team, TeamInvite, TeamMember, User
```

Enums: `AuthEventType`, `AuthType`, `CancellationReason`, `Plan`,
`RecipientType`, `TeamMemberRole`.

Aggregate roots the mobile API will care about most:

- **`User`** — the account. Owns mailboxes, threads, subscription,
  team memberships.
- **`MailAccount`** — a connected inbox (IMAP-password or Gmail-OAuth).
  Encrypted creds; per-account polling policy, last-seen UID, sent-
  folder cursor, and signature.
- **`EmailThread`** — grouped by `conversationKey` (normalized
  participant set) since V32; keeps unread flag per owner.
- **`Message`** — a single email, with `outbound` flag (V30), a link
  to the sender `Participant`, and `MessageRecipient` rows for
  To/Cc/Bcc.
- **`Subscription`** — Stripe-mirrored plan/status/period; drives
  plan-limit enforcement.

## DB schema overview

PostgreSQL 16 in prod and local; H2 for tests. Schema is Flyway-owned:
migrations under `src/main/resources/db/migration/V{n}__*.sql`,
**immutable once merged**. `V1__init.sql` bootstraps thread+message+
participant; later `V2..V35` add auth, subscriptions, mailboxes,
teams, saved searches, digests, delivery-state fields, per-account
signature, and the current `conversationKey` grouping. Migrations run
on startup.

Local dev bootstraps the DB + app user via
`src/main/resources/db/init.sql` (see README). Same env-var pattern as
`tax-tracker`: `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`,
`JDBC_DATABASE_PASSWORD`.

## Auth model

- **Web**: Spring Security form login on `POST /login`, session cookie
  (`JSESSIONID`); CSRF on. Google OIDC via
  `spring-boot-starter-oauth2-client` at `/auth/google/start` →
  `/login/oauth2/code/google`. Password hashing: BCrypt. Sessions live
  in the servlet container (in-memory) — single-node deploy.
- **Admin gating**: `application.yml`'s `admin.emails` allowlist; a
  non-admin hitting an `/admin/**` URL gets 404, not 403 (no signal to
  scan for admin surface).
- **Mailbox creds**: encrypted at rest with a key derived from
  `MAILBOX_ENCRYPTION_PASSWORD` + `MAILBOX_ENCRYPTION_SALT`; fallback
  key in dev only, warned about at boot.
- **Mobile (planned)**: distinct token-based auth under
  `/api/mobile/v1/**` (bearer token, opaque; issued on
  `POST /api/mobile/v1/auth/login`; stored server-side in a
  `mobile_session` table added by a future migration). Not
  implemented yet — see `docs/API.md` § "Planned mobile API".

## How the website relates to the mobile REST API

The Thymeleaf website is at the "flat" URL space (`/chats`,
`/mailboxes`, `/account`, `/billing`, ...). The mobile REST API is
strictly at `/api/mobile/v1/**` and is a JSON-only surface. They MUST
share the service layer: any state mutation the mobile client can
trigger must go through the same service method the web controller
calls — never a parallel implementation. If a controller reaches
directly into a repository, that's a smell; refactor into the service
first.

Rationale: plan-limit checks, quota decrements, audit `AuthEvent`
writes, and IM-transform rules already sit behind the service methods
today. Duplicating them in a mobile controller is how divergence
starts.

## Live delivery pipeline (2026-08-10)

- `MailboxPollingService` runs `@Scheduled`; for accounts on paid
  tiers with `mailbox.idle.enabled=true` it hands off to
  `ImapIdleService` for push-style IMAP IDLE.
- New commits publish `NewMailEvent` (Spring app event, per-user
  scoped).
- `ChatEventStream` subscribes user emitters; `ChatEventsController`
  exposes `GET /chats/events` as `text/event-stream` for the web PWA.
- The mobile API will reuse the same event bus behind a
  `GET /api/mobile/v1/events` SSE endpoint (planned) plus Web
  Push/APNs/FCM for background delivery.

## Tests

- `./mvnw test` runs the JUnit 5 suite. H2 for slice tests; Postgres
  Testcontainers for integration tests that must exercise real SQL.
- GreenMail for SMTP/IMAP integration tests around the polling and
  reply paths.
- Migration is verified on every boot; a broken migration fails the
  test suite before any assertion runs.
