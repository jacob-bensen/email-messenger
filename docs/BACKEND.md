# BACKEND — architecture

Repo classification: BACKEND (pom.xml + Spring Boot + Flyway under `src/main/resources/db/migration`).
Note: the actual stack differs from the generic routine template — persistence is **Spring Data JPA**
(not jOOQ); there is no jOOQ codegen. See `CLAUDE.md` for the authoritative stack.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PwaController`, `SeoController`, `LegalController`, `PricingController`), `ChatEventStream` (SSE), `ChatService`, `ConversationListService`, `OutboundMessageService`, `GlobalExceptionHandler` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view-model records (`ChatConversation`, `BubbleRun`, …) |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), `CredentialEncryptor` |
| `auth` | Form login + Google OIDC, registration, verification, password reset, throttling (`LoginThrottleFilter`), account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), banners, trial conversion |
| `team` | Teams + invites |
| `digest`, `activation`, `reengagement` | Scheduled emails |
| `admin` | `/admin/revenue` operator metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Persistence
PostgreSQL 16 (prod/local), H2 for tests. Flyway `V1`–`V35` (immutable once merged; add `V36+`).
Core tables: `users`, `subscriptions`, `mail_accounts` (encrypted creds, `auth_type`, last-seen UID cursors,
signature), `email_thread` (owner, `conversation_key`, unread), `message` (+ outbound flag), `message_recipient`,
`participant`, `attachment` (`blob_ref` always null today — attachment bytes are not stored),
`saved_searches`, teams/invites, auth_events, plan_change_events, password/email tokens, remember-me tokens.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`/`password`) or Google OIDC; remember-me
(30 d, persistent token repo). **CSRF is ON** except `/h2-console/**` and `/billing/webhook`.
Everything not in the `permitAll` list in `SecurityConfig` requires authentication. Controllers resolve the user
with `userService.requireByEmail(principal.getName())`. **There is no token/bearer auth** — a mobile client
cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs API
All HTML pages; the only JSON/non-HTML endpoints are `POST /chats/refresh`, `GET /chats/events` (SSE) and
`POST /billing/webhook`. There is no `/api/**`. Future REST controllers must call the same services the
Thymeleaf controllers use (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`).
Known logic living in controllers (drift risk): mailbox scope selection (`selectMailbox`/`scopeAddress`) and
send-path recipient/subject handling in `ConversationController`.

## Live delivery
`MailboxPollingService` / `ImapIdleService` publish `NewMailEvent` → `ChatEventStream` → SSE `/chats/events`.
