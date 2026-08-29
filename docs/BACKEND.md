# BACKEND.md — email-messenger (ConexusMail server)

This is the authoritative Spring Boot server for the ConexusMail product.
It owns:

- The **public website** (Thymeleaf pages under
  `src/main/resources/templates/`) — landing, pricing, legal, auth flows,
  billing, the `/chats` texting-style workspace, mailbox management, and
  admin metrics.
- **All persistence** (PostgreSQL 16 in prod, H2 in tests; Flyway
  migrations in `src/main/resources/db/migration/`).
- **All authoritative business rules** — plan limits, billing, mailbox
  polling, IM transform, threading, and (eventually) the mobile REST API.

## Module layout (`com.emailmessenger.*`)

| Package | Responsibility |
| --- | --- |
| `EmailMessengerApplication` | Boot entry; enables `@Scheduling`, `@Async`, config properties. |
| `activation`  | Post-signup activation nudge scheduling. |
| `admin`       | Revenue / churn / funnel / retention metrics + admin-only pages. |
| `auth`        | User accounts, form login, Google OIDC, email verification, password reset, throttling, appearance/notification settings. |
| `billing`     | Stripe checkout, portal, webhook, plan/period tracking, cancel flow. |
| `digest`      | Weekly digest email preferences + opt-out. |
| `domain`      | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team`, …) and enums (`Plan`, `MailAuthType`, …). |
| `email`       | IMAP / SMTP integration: polling, sent-folder sync, IDLE, OAuth token refresh, mailbox lifecycle. |
| `reengagement`| Scheduled outreach for lapsed users. |
| `repository`  | Spring Data JPA repositories. |
| `service`     | Non-email core services: threading, IM transform (`EmailBodyCleaner`, `ChatConversation`), reply composition, conversation-key derivation, chat event bus. |
| `team`        | Team creation + invite flow. |
| `web`         | Thymeleaf controllers, mailbox management pages, `/chats` workspace, PWA endpoints, SEO. |

## Storage

- Postgres 16 (Flyway migrations `V1__init.sql` … `V35__mail_account_signature.sql`).
- Same connection env vars as sibling projects: `JDBC_DATABASE_URL`,
  `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`. Local profile in
  `application-local.yml` points at `localhost:5432/email_messenger` and
  `db/init.sql` bootstraps it.
- Tests run against H2 via `src/test/resources/application.yml`.

The routine prompt calls out jOOQ as the target ORM, but the code today
uses **JPA** (`spring-boot-starter-data-jpa`). Treat JPA as the actual
persistence layer; the docs follow the code, not the aspiration.

## Auth model

- Form login + Google OIDC (`spring-boot-starter-oauth2-client`), configured
  in `auth/SecurityConfig`.
- Session-based (Spring Security default): the website relies on the
  session cookie plus CSRF tokens.
- No mobile-facing auth exists yet (see `docs/API.md`).

## Mailbox integration (`email/*`)

- Password IMAP + Gmail OAuth (per-account `MailAuthType`).
- `MailboxPollingService` scheduled poll + sent-folder sync (cursor in
  `V31__mail_account_last_seen_sent_uid.sql`).
- `ImapIdleService` for paid tiers pushes new mail into
  `NewMailEvent`, which `ChatEventStream` fans out over
  `/chats/events` (SSE).
- OAuth token refresh: `GmailReconnectController` for stale refresh
  tokens (Google Testing-mode workaround — see backend CLAUDE.md).

## The `/chats` workspace

Conversations are keyed by the set of participants
(`ConversationKeyService`) rather than by mail thread. That merges the
inbound `EmailThread`s and the outbound sent copies for the same set of
people into one bubble stream, and is what the mobile client will render.

## Payment / plans

- `Plan` enum: `FREE`, `PRO`, `BUSINESS`.
- Stripe drives entitlement; `Subscription`, `PlanChangeEvent`, and the
  billing-period fields on `Subscription` are the source of truth.

## Admin

`/admin/revenue` (Spring Security-gated by `AdminAuthorizer` against
`AdminProperties.emails`) shows revenue, churn, activation funnel,
trial-end conversion, win-back conversion, and at-risk retention
tables. `AdminWeeklyDigestScheduler` emails a summary weekly.

## What is NOT here yet

- **Public REST API** for the mobile client. Only endpoint outside HTML
  today is `POST /billing/webhook` (Stripe). Everything else returns HTML
  or SSE. See `docs/API.md` for the design of `/api/v1/*`.
- **Web Push** delivery (roadmap in the top-level `CLAUDE.md`).
- **Delivery states on outbound bubbles** (roadmap).
