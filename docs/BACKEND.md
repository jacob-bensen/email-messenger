# Backend architecture

Classified as **BACKEND** by evidence: `pom.xml`, Spring Boot 3 annotations, Flyway migrations
(`V1`–`V35`). Differences from the generic target stack: persistence is **Spring Data JPA /
Hibernate** (`ddl-auto: validate`), not jOOQ; there is no codegen step. Java 21, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, mailbox/saved-search/PWA/SEO/legal controllers, `ChatEventStream` SSE) |
| `service` | Mail-to-chat transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view-model records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`, `ChatMember`) |
| `email` | IMAP/SMTP integration: import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IDLE (`ImapIdleService`), Gmail OAuth, credential encryption |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security (`SecurityConfig`), form login + Google OIDC, registration, password reset, email verification, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `reengagement`, `activation`, `admin` | Teams/invites, digest emails, lifecycle emails, operator metrics (`/admin/**`, allowlist) |

## Core model
A **conversation** is keyed by `conversationKey` (hash of the participant set, V32) and merges
all `EmailThread`s with that participant set into one timeline. Messages hold sender,
recipients (`MessageRecipient`), html/plain body, `sentAt`, `outbound` flag (V30), attachments
(metadata only; `blob_ref` is always null today).

## Schema overview (Flyway, immutable once merged)
V1 init (threads/messages/participants/attachments) · V2 users · V3 thread owner · V4
subscriptions (+V17 billing period, V23/V28 email tracking, V27 cancellation reason) · V5–V7
mail accounts + polling policy · V9 unread · V10/V11 saved searches · V12/V13/V20–22 digest and
lifecycle tracking · V14/V15 reset/verification tokens · V16 auth events · V18/V19 Google
subject / password-set · V24 teams · V26 plan-change events · V29 mail account auth type ·
V30 outbound messages · V31 sent-UID cursor · V32 conversation key · V34 mail preferences ·
V35 mailbox signature.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`,`password`) or Google
OIDC; remember-me (30 d, persistent token table). **CSRF enabled** except `/h2-console/**` and
`/billing/webhook`. Everything not whitelisted in `SecurityConfig` requires authentication.
No token (bearer/JWT) auth exists yet — a mobile client cannot authenticate non-interactively.
`/admin/**` is allowlisted by email (`admin.emails`); non-admins get 404.

## Web vs REST
The app is server-rendered. Controllers call `ChatService` / `ConversationListService` /
`ReplyService` / `OutboundMessageService` and put results in the Thymeleaf `Model`. Only
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE) and `POST /billing/webhook` are
non-HTML. See `API.md` for the contract and the planned `/api/v1`.

Note: `ConversationController` still contains orchestration logic (choose sending account,
signature append, reply-vs-new decision, recipients) that a REST controller would need — it
should be extracted into a service before `/api/v1` is built (see INTERNAL_TODO.md).
