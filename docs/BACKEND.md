# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail", repo `email-messenger`). Reverse-engineered
from the code on 2026-10-18; anything not verified is marked TBD.

> Deviation from the sync-routine's assumed stack: persistence is **Spring Data JPA**
> (Hibernate) with Flyway, **not jOOQ**. There is no codegen step.

## Modules (`com.emailmessenger.*`)

| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account pages, login throttling/audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial/upgrade nudges |
| `email` | Mailbox connect (IMAP password, Gmail OAuth), polling import, IMAP IDLE, MIME parsing, conversation-key backfill |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, view-model records |
| `web` | Thymeleaf controllers for chats, mailboxes, saved searches, pricing/landing/legal, PWA, SSE (`ChatEventStream`) |
| `team` | Team invites and membership |
| `digest`, `activation`, `reengagement` | Scheduled emails (weekly digest, activation, win-back) |
| `admin` | Operator revenue/retention metrics pages |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Persistence

Flyway migrations in `src/main/resources/db/migration` (`V1`…`V35`, immutable once merged).
Tables: users, mail_accounts, email_threads, messages, message_recipients, participants,
attachments (metadata only; `blob_ref` is always null), subscriptions, plan_change_events,
teams, team_members, team_invites, saved_searches, thread_notes, digest_email_preferences,
email_verifications, password_reset_tokens, persistent_logins, auth_events.
Postgres 16 in prod/local, H2 in tests.

## Auth model

- Session cookie (`JSESSIONID`) via form login (`POST /login`, fields `email`, `password`) or
  Google OIDC (`/auth/google/start`). Remember-me cookie, 30 days.
- CSRF is **on** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the public allow-list in `SecurityConfig` requires authentication.
- **There is no token/bearer authentication.** A mobile client cannot authenticate today.

## Web vs. API

The server is a server-rendered website. Controllers return Thymeleaf view names and use
redirect-after-POST. The only JSON/stream endpoints are `POST /chats/refresh`
(`{"imported": n}`), `GET /chats/events` (SSE) and `POST /billing/webhook` (Stripe). There is
no REST API for the mobile client yet — see `docs/API.md`. Business logic lives in
`service`/`email`/`billing` classes that controllers call; the planned `/api/v1` layer must
call the same services (notably `ConversationService`, `ChatService`, `OutboundMessageService`,
`ReplyService`, `MailAccountService`, `PlanLimitService`).
