# BACKEND.md — architecture

Spring Boot 3 / Java 21 / Maven. Server-rendered Thymeleaf website ("ConexusMail") plus
background jobs. Sibling mobile client: `conexus-mail-mobile` (Flutter, currently a stub).

> Note vs. the generic routine stack: persistence is **Spring Data JPA + Flyway on PostgreSQL**
> (H2 in tests), **not jOOQ**. No jOOQ codegen exists. Recorded in INTERNAL_TODO.md.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers (chats, mailboxes, saved searches, legal, SEO, PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service/` | Email→IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records |
| `domain/` | JPA entities: `User`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `MailAccount`, `Subscription`, `Team*`, `SavedSearch`, token/audit entities |
| `repository/` | Spring Data repositories |
| `email/` | IMAP/Gmail integration: import, polling, IDLE, MIME parsing, credential encryption |
| `auth/` | `SecurityConfig`, registration/login, Google OIDC, password reset, email verification, account pages |
| `billing/` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` | Teams/invites, weekly digests, lifecycle emails, admin metrics |

## Schema
Flyway migrations `src/main/resources/db/migration/V1..V35` (immutable once merged). Core tables:
users, email_thread (owner, `conversation_key`, unread), message (outbound flag), participants,
attachments (`blob_ref` always null — bytes not stored), mail_accounts (IMAP/Gmail OAuth, cursors
`last_seen_uid`, `last_seen_sent_uid`), subscriptions, teams/invites, saved_searches, auth_events.

## Auth model
Session-cookie auth only (Spring Security): form login (`/login`, params `email`,`password`,
remember-me 30d via persistent token table) and optional Google OIDC (enabled when
`GOOGLE_CLIENT_ID` set). CSRF **enabled** except `/h2-console/**` and `/billing/webhook`.
Everything not whitelisted in `SecurityConfig` requires authentication.
**There is no token/JWT auth and no JSON REST API for a mobile client yet** (see API.md).

## Web ↔ API relationship
All current endpoints are HTML/redirect endpoints (see WEB.md). Only JSON-ish responses:
`POST /chats/refresh` (`{"imported":n}`), SSE `GET /chats/events`, `POST /billing/webhook`.
A future `/api/v1/**` layer must call the same services (`ChatService`, `ConversationListService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`) as the controllers.
