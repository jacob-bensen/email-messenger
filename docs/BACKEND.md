# BACKEND

Reverse-engineered from the code on 2026-10-24. Items not verified are marked TBD.

## Classification note
`email-messenger` is the BACKEND (Spring Boot, Flyway, Maven, Thymeleaf).
Deviation from the generic target stack: persistence is **Spring Data JPA /
Hibernate** (`ddl-auto: validate`), not jOOQ. There is no codegen step.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Security (form login + Google OIDC +
persistent remember-me), Spring Data JPA, Flyway (35 migrations, V1..V35),
PostgreSQL 16 (prod/local), H2 (tests), Stripe, Jakarta Mail (IMAP/SMTP).

## Packages (`com.emailmessenger`)
- `web/` – Thymeleaf controllers (chats workspace, mailboxes, saved searches,
  pricing, PWA, SEO, legal), `ChatEventStream` (SSE), view services.
- `auth/` – SecurityConfig, registration, login throttling, password reset,
  email verification, Google OIDC, account pages.
- `billing/` – Stripe checkout/portal/webhook, plan limits (`PlanLimitService`).
- `email/` – IMAP import, polling (`MailboxPollingService`), IMAP IDLE,
  Gmail OAuth, credential encryption, MIME parsing.
- `service/` – conversation building, IM transform, body cleaning, replies.
- `domain/`, `repository/` – JPA entities and repositories.
- `team/`, `digest/`, `reengagement/`, `activation/`, `admin/` – teams/invites,
  digests and schedulers (off unless `*.enabled`), operator metrics.

## Data model (see `src/main/resources/db/migration`)
users, subscriptions, mail_accounts (IMAP password or Gmail OAuth; credentials
encrypted), email_thread (carries `conversationKey` = participant set),
message / message_recipient / participant / attachment (`blob_ref` always null:
bytes not stored), saved_search, teams/invites, auth_events, plan_change_events,
digest preferences. Flyway migrations are immutable once merged.

## Auth model
Browser session cookie (JSESSIONID) + `remember-me` (30 days, JDBC token
repository). CSRF **enabled** except `/h2-console/**` and `/billing/webhook`.
Public routes are listed in `auth/SecurityConfig.java`; everything else
requires authentication. `/admin/**` is gated by `admin.emails` allowlist
(non-admins get 404). **There is no token (bearer/JWT) auth** – see
INTERNAL_TODO.md.

## Web vs. REST
The backend currently serves only server-rendered pages plus three non-HTML
endpoints (`POST /chats/refresh` JSON, `GET /chats/events` SSE,
`POST /billing/webhook`). There is no REST API for the mobile client yet;
`docs/API.md` records exactly what exists and what is planned.
Business logic lives in services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `ReplyService`, `MailboxPollingService`, ...);
controllers are thin, so a future `/api/v1` controller layer must call the same
services.

## Config
Env vars: `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_CLIENT_ID/SECRET`,
`STRIPE_*`, `MAILBOX_ENCRYPTION_PASSWORD/SALT`, `MAILBOX_POLLING_ENABLED`,
`MAILBOX_IDLE_ENABLED`.
