# Backend architecture

Reverse-engineered 2026-10-23 from the code. Unknowns are marked TBD.

**Stack (actual):** Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ), Flyway
(35 migrations, `V1`–`V35`), PostgreSQL (H2 in tests), Thymeleaf, Maven wrapper,
Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers for the chat workspace (`ConversationController`), mailboxes, saved searches, landing/pricing/legal, PWA shell, SEO, SSE (`ChatEventsController`). View-model services: `ChatService`, `ConversationListService`, `OutboundMessageService`. |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig`. |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial conversion. |
| `team` | Teams and invites. |
| `email` | Mailbox integration: IMAP client, IDLE, polling scheduler/service, import (`EmailImportService`), MIME parsing, Gmail OAuth, credential encryption. |
| `service` | Conversation logic: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`. |
| `domain` / `repository` | JPA entities / Spring Data repositories. |
| `admin`, `digest`, `activation`, `reengagement` | Admin revenue metrics, digest emails, lifecycle emails. |

## Data model (key entities)
`User`, `Subscription` (`Plan` FREE/PRO/BUSINESS), `MailAccount` (IMAP password or
Gmail OAuth, `last_seen_uid`, `last_seen_sent_uid`, signature), `EmailThread`
(has `conversationKey` = participant-set key, owner, unread), `Message` (inbound/outbound),
`Participant`, `MessageRecipient`, `Attachment` (metadata only; `blob_ref` always null),
`SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, `AuthEvent`, `PlanChangeEvent`, token tables.

## Auth model
Session cookie (form login, remember-me, optional Google OIDC). CSRF enabled for all
routes except `/h2-console/**` and `/billing/webhook`. Everything not on the public
allow-list in `SecurityConfig` requires authentication. **No token/Bearer auth exists.**

## Web vs. API
There is **no REST API**. All UI is server-rendered Thymeleaf; controllers call services
directly. The only JSON/stream endpoints are `POST /chats/refresh` (JSON) and
`GET /chats/events` (SSE). See `docs/API.md` and `docs/WEB.md`.

## Live updates
`MailboxPollingService` publishes `NewMailEvent(userId, importedCount)`; `ChatEventStream`
fans it to SSE subscribers as event name `new-mail`. IDLE for paid tiers behind
`mailbox.idle.enabled`.
