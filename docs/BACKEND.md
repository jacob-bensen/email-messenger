# Backend architecture

Classification: BACKEND repo (Maven + Spring Boot 3 + Flyway; ~35 migrations).
Sibling: `conexus-mail-mobile` (Flutter client, see its `docs/`).

> Deviation from the generic routine: persistence is **Spring Data JPA /
> Hibernate**, not jOOQ. There is no jOOQ codegen. Follow the JPA conventions in
> `CLAUDE.md`.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`; view-model records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`) |
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `email` | IMAP/SMTP/Gmail OAuth: `EmailImportService`, `MailboxPollingService`, `ImapIdleService`, `MimeMessageParser`, `MailAccountService` |
| `auth` | Spring Security config, registration, password reset, email verification, Google OIDC, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, lifecycle nudges, operator metrics |

## Auth model (today)
- Form login (`POST /login`, params `email`, `password`) → server session
  (`JSESSIONID`) + persistent remember-me cookie (30 days, JDBC token repo).
- Google OIDC login when `GOOGLE_CLIENT_ID` is set.
- CSRF **on** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the public allow-list in `SecurityConfig` requires auth.
- **No token/bearer auth exists.** A mobile client cannot authenticate
  non-interactively yet (see `INTERNAL_TODO.md`).

## Data model overview
`User` 1—N `MailAccount` (IMAP password or Gmail OAuth; credentials encrypted by
`CredentialEncryptor`). Imported mail: `EmailThread` 1—N `Message`, `Message`
N—1 `Participant` (sender), `MessageRecipient`, `Attachment` (metadata only;
`blob_ref` always null). A chat conversation is identified by `conversationKey`
(participant-set hash), merged across threads at read time. Billing:
`Subscription`, `PlanChangeEvent`; plans Free/Pro/Business enforced by
`PlanLimitService`.

## Web vs. API
The app is server-rendered only. `/chats/**` controllers call `ChatService`,
`ConversationListService`, `OutboundMessageService` and return Thymeleaf views.
The only JSON-ish endpoints are `POST /chats/refresh` and the Stripe webhook;
`GET /chats/events` is SSE. A REST layer for mobile should be a thin controller
set over the **same** services (see `API.md` planned section).

## Live updates
`MailboxPollingService` / `ImapIdleService` publish `NewMailEvent`;
`ChatEventStream` fans out to per-user `SseEmitter`s.
