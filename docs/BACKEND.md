# Backend architecture

Repo classification: BACKEND. Evidence: `pom.xml` (Spring Boot 3, Java 21), Spring
annotations throughout `src/main/java`, Flyway migrations V1–V35 under
`src/main/resources/db/migration`, Thymeleaf templates under `src/main/resources/templates`.

> Note: the routine's default stack says jOOQ. This codebase uses **Spring Data JPA**
> (Hibernate, `ddl-auto: validate`) — there is no jOOQ and no codegen step.

## Stack
Java 21, Spring Boot 3, Spring Security (form login + Google OIDC + persistent
remember-me), JPA/Hibernate, Flyway, PostgreSQL 16 (H2 in tests), Jakarta Mail
(IMAP/SMTP), Stripe, Thymeleaf, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers + view services: `ConversationController` (/chats), `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, SSE (`ChatEventsController`, `ChatEventStream`), PWA/SEO/legal/landing |
| `service` | Pure transforms and view records: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP), records `ChatConversation`, `ChatMember`, `BubbleRun`, `BubbleMessage`, `ConversationListItem` |
| `email` | Mailbox integration: IMAP polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, `EmailImportService`, `MimeMessageParser`, `MailAccountService`, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, login throttle, password reset, email verification, account settings, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), banners, trial nudges |
| `team` | Team invites/membership |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails (each off unless its `*.enabled` flag is true) |
| `admin` | `/admin/**` metrics, gated by `admin.emails` allowlist (404 otherwise) |

## Core domain
- `EmailThread` (owned by a `User`, carries `conversationKey`), `Message` (+ `MessageRecipient`,
  `Attachment`, `outbound` flag), `Participant` (deduped by normalized email), `MailAccount`
  (IMAP creds encrypted at rest; `auth_type` password|Gmail OAuth; UID cursors `last_seen_uid`,
  `last_seen_sent_uid`), `User`, `Subscription`/`Plan` (FREE/PRO/BUSINESS), `Team*`, `SavedSearch`.
- **Conversation** = all messages sharing a *participant-set key* (`conversationKey`), merged
  across email threads into one timeline of `BubbleRun`s (consecutive same-sender bubbles).
- Attachments: metadata only; `blob_ref` is always null (not downloadable yet — roadmap).

## Auth model
Browser session cookie (`JSESSIONID`) + CSRF tokens (on by default; exempt: `/h2-console/**`,
`/billing/webhook`) + optional remember-me (30 days, DB-backed). Everything not in the
`SecurityConfig` permit-list requires authentication. **There is no token/Bearer auth** —
see `INTERNAL_TODO.md` (mobile needs one).

## Web vs REST
All HTTP surface today is server-rendered Thymeleaf (see `WEB.md`). The only JSON/stream
endpoints are `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) and
`POST /billing/webhook` (Stripe). There is **no `/api/**` namespace yet**.
Planned mobile API: `API.md` §Planned. Rule: API controllers must call the same services
the Thymeleaf controllers use (`ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`).

## Known layering smell
`ConversationController.send/createMessage` orchestrates `ReplyService` +
`OutboundMessageService` + signature + recipient parsing inline (private helpers in the
controller). That logic must be extracted to a service before an API can share it.
