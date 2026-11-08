# BACKEND.md — architecture (reverse-engineered 2026-11-08)

Spring Boot 3 / Java 21 / Maven monolith. **Persistence is Spring Data JPA + Flyway (not jOOQ).**
PostgreSQL 16 in prod/local, H2 in tests. Server-rendered Thymeleaf UI; there is currently **no JSON REST API** (see `API.md`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, saved searches, landing/pricing/legal, PWA, SEO), `ChatEventStream` SSE |
| `auth` | Spring Security config, form login + Google OIDC, registration, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP/SMTP: import (`EmailImportService`), polling, IMAP IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService`, view-model records |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digests, lifecycle emails, operator metrics |

## Data model (35 Flyway migrations, `V1`..`V35`, see `src/main/resources/db/migration`)
`User`, `Subscription`/`Plan`/`PlanChangeEvent`, `MailAccount` (encrypted creds, polling cursor, signature),
`EmailThread` (+ `conversationKey` = participant-set key), `Message`, `MessageRecipient`, `Participant`, `Attachment`
(`blob_ref` always null — bytes not stored), `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, token tables, `AuthEvent`.
Migrations are immutable; add a new `V{n}`.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`,`password`) or Google OIDC; remember-me cookie (30d, JDBC token repo).
CSRF is **on** (exempt: `/billing/webhook`, `/h2-console/**`). Everything not in the `permitAll` list in `auth/SecurityConfig` requires login.
No bearer/JWT token auth exists → a native mobile client cannot authenticate non-interactively today (see `INTERNAL_TODO.md`).

## Web vs REST
All controllers are `@Controller` returning views; only `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) and `POST /billing/webhook` return non-HTML.
Controllers delegate to services (`ConversationListService`, `ChatService`, `OutboundMessageService`, `MailAccountService`, …); future JSON endpoints must reuse these.

## Live delivery
`MailboxPollingService` imports and publishes `NewMailEvent` → `ChatEventStream` emits SSE `new-mail`. `ImapIdleService` (paid plans, flag `mailbox.idle.enabled`).
