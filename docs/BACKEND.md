# Backend architecture

Repo classification: BACKEND (Spring Boot 3 / Java 21, Flyway migrations `V1`–`V35`).
Sibling: `conexus-mail-mobile` (Flutter client, currently an empty scaffold).

> Deviation from the generic stack template: persistence is **Spring Data JPA /
> Hibernate** (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen step.

## Package layout (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, legal, SEO, PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE) |
| `service` | Conversation/IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, bubble/run view records |
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, tokens, audit events |
| `email` | IMAP/Gmail integration: `MailboxPollingService/Scheduler`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor` |
| `auth` | Spring Security config, form login + Google OIDC, registration, verification, password reset, login throttle, account pages |
| `billing` | Stripe checkout/portal/webhook, `Plan` limits (`PlanLimitService`, kinds: THREAD_COUNT, MAILBOX_COUNT, SAVED_SEARCH_COUNT) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle nudges, admin revenue/retention metrics |

## Data model (overview)

- `User` (plan FREE/PRO/BUSINESS) owns `MailAccount`s (IMAP password or Gmail OAuth; credentials encrypted; per-account `lastSeenUid` / `lastSeenSentUid` cursors, polling backoff).
- `EmailThread` (owner, subject, `conversationKey` = participant-set key, `unread`, `messageCount`) → `Message` (Message-ID/In-Reply-To, plain+html body, `sentAt`, `outbound`) → `MessageRecipient`, `Attachment` (`blob_ref` always null: bytes not stored).
- `/chats/{key}` merges all threads sharing a `conversationKey` into one timeline.

## Auth model

Session cookie (`JSESSIONID`) + remember-me (30 d), CSRF **on** (exempt only `/h2-console/**`, `/billing/webhook`). Everything not on the permitAll list in `SecurityConfig` requires authentication. Form login: `POST /login` (`email`, `password`). Google OIDC optional. **No token/Bearer auth exists** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST

There is **no REST API**. All user-facing behavior is server-rendered Thymeleaf; only three endpoints return non-HTML (see `API.md`). Business logic is partly in services and partly inline in `ConversationController` (see INTERNAL_TODO "logic drift").

## Live updates

`MailboxPollingService` publishes `NewMailEvent`; `ChatEventStream` fans out to per-user SSE emitters (`GET /chats/events`). `ImapIdleService` (paid tiers, `mailbox.idle.enabled`) triggers polling on IDLE.
