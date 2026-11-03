# BACKEND — architecture

Status: reverse-engineered 2026-11-03 from the code. Unknowns are marked TBD.

Spring Boot 3 / Java 21 app ("ConexusMail", package `com.emailmessenger`) that
imports email over IMAP and renders it as an instant-message-style chat. It is
a **server-rendered monolith**: the website (Thymeleaf) is the only client
today. There is **no REST/JSON API yet** (see `docs/API.md`).

> Deviation from the generic routine template: persistence is **Spring Data
> JPA/Hibernate**, not jOOQ. There is no jOOQ codegen step. Follow the JPA
> conventions already in the repo.

## Packages (`src/main/java/com/emailmessenger/`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, saved searches, pricing, legal, PWA, SEO), `ChatService`/`ConversationListService`/`OutboundMessageService`, SSE (`ChatEventStream`) |
| `service` | Conversation model: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | IMAP/Gmail integration: `ImapClient`, `EmailImportService`, `MimeMessageParser`, `MailboxPollingService`/`Scheduler`, `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security config, registration/login/password reset/email verification, Google OIDC, login throttling, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, Free/Pro/Business) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, operator revenue/churn metrics |

## Persistence

PostgreSQL 16 (prod/local), H2 for tests. Flyway migrations
`src/main/resources/db/migration/V1..V35` (immutable once merged; next is
`V36`). Core tables: `users`, `persistent_logins`, `mail_accounts`,
`email_threads` (has `conversation_key`, owner), `messages` (has outbound
flag), `message_recipients`, `participants`, `attachments` (`blob_ref` always
null — bytes are not stored), `subscriptions`, `plan_change_events`,
`saved_searches`, `teams`/`team_members`/`team_invites`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`,
`auth_events`.

## Conversation model

A chat is keyed by `conversationKey` = participant-set of the thread
(`ConversationKeyService`); `/chats/{key}` merges all threads with that key.
Messages become bubble runs (consecutive same sender), quoted replies stripped
(`EmailBodyCleaner`).

## Auth model

`auth/SecurityConfig`: everything except a public allow-list requires
authentication. Form login (`POST /login`, params `email`, `password`),
session cookie `JSESSIONID`, persistent remember-me (30 days, DB-backed),
optional Google OIDC login when `GOOGLE_CLIENT_ID` is set, CSRF **enabled**
(exempt only `/h2-console/**`, `/billing/webhook`), login throttling filter.
**No bearer/token auth exists.** Mobile auth strategy: TBD (planned in
`INTERNAL_TODO.md`).

## Web vs. API

Only the web layer exists. Controllers call services directly. The few
JSON/stream endpoints: `POST /chats/refresh` (JSON `{imported:n}`),
`GET /chats/events` (SSE `new-mail`), `POST /billing/webhook`. When a REST API
is added it must reuse `ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, `SavedSearchService` — not copy
controller logic (note: `ConversationController.send` currently contains send
orchestration that should move into a service first; see INTERNAL_TODO).

## Build / test

See `CLAUDE.md`. Related repo: `conexus-mail-mobile` (Flutter client).
