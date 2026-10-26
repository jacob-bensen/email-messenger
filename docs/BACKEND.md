# BACKEND — architecture

Repo: `email-messenger` (ConexusMail). Spring Boot 3 / Java 21 / Maven, server-rendered
Thymeleaf UI. Reverse-engineered 2026-10-26; items not verified are marked TBD.

> Deviations from the generic sync-routine assumptions: persistence is **Spring Data
> JPA/Hibernate (not jOOQ)**, so there is no jOOQ codegen step. Flyway + PostgreSQL 16
> (H2 in tests) as assumed.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, PWA, SEO, legal), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation view models/transforms: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send) |
| `email` | IMAP/SMTP integration: `EmailImportService`, `MimeMessageParser`, `MailboxPollingService`, `ImapIdleService`, `GmailOAuthClient`, `MailAccountService`, `CredentialEncryptor` |
| `auth` | Spring Security config, registration/login, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits`/`PlanLimitService` (FREE 3 mailboxes/500 threads, PRO 5 mailboxes, BUSINESS unlimited) |
| `team` | Teams + invites |
| `digest`, `reengagement`, `activation` | Email schedulers (flag-gated via `*.enabled`) |
| `admin` | `/admin/**` metrics, allowlisted via `admin.emails` (non-admin gets 404) |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)
`users`, `subscriptions` (+plan change events), `mail_accounts` (IMAP password or Gmail
OAuth, encrypted credentials, polling policy, last-seen UID cursors, signature),
`email_threads` (owner, unread, `conversation_key`), `messages` (outbound flag),
`participants` (deduped by normalized address), `message_recipients`, `attachments`
(`blob_ref` always null — bytes not stored), `saved_searches`, `teams`/`team_members`/
`team_invites`, token tables (password reset, email verification), `auth_events`,
`digest_email_preferences`, remember-me persistent tokens.

Core concept: **conversationKey** = key derived from the participant set (excluding the
owner's own addresses); all threads with the same key merge into one chat timeline.

## Auth model
Session cookie (JSESSIONID) + optional remember-me (30d, JDBC token store), CSRF enabled
(exempt: `/h2-console/**`, `/billing/webhook`). Form login at `POST /login` (`email`,
`password`); Google OIDC when `GOOGLE_CLIENT_ID` is set. Login throttle filter in front of
form login. **There is no token (Bearer/JWT) auth** — a mobile client cannot authenticate
today (see `INTERNAL_TODO.md`).

## Web ↔ API relationship
Today there is **no REST API**; the only JSON/ResponseBody endpoints are
`POST /chats/refresh` and the SSE stream `GET /chats/events`. All other controllers return
Thymeleaf views. Plan: the mobile REST API (`/api/v1/**`, see `API.md`) must call the same
services as the web controllers. Some logic currently lives privately inside
`ConversationController` (recipient parsing, `sendingAccount`, `withSignature`,
`selectMailbox`) and must be extracted to a service before an API reuses it.

## Scheduling / live delivery
`MailboxPollingScheduler` (flag `mailbox.polling.enabled`), `ImapIdleService` for paid
tiers (`mailbox.idle.enabled`), publishes `NewMailEvent` → `ChatEventStream` SSE.
