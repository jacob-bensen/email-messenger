# BACKEND.md — architecture

Reverse-engineered 2026-10-08 from the code. "TBD" = not determined/implemented.

## Classification
`email-messenger` is the BACKEND (pom.xml, Spring Boot, Flyway migrations, Thymeleaf).
Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate, not jOOQ**
(no codegen step). Java 21, Spring Boot 3, PostgreSQL 16 (H2 in tests), Flyway V1..V35, Maven.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities: `User`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `MailAccount`, `Subscription`, `Plan`, `Team*`, `SavedSearch`, `AuthEvent`, tokens |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail integration: `MailboxPollingService` (+scheduler), `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `MailAccountService`, `CredentialEncryptor` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `web` | Thymeleaf controllers + web-facing services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `OnboardingService`), SSE (`ChatEventStream`) |
| `auth` | Spring Security config, registration, password reset, email verification, Google OIDC, login throttling, audit |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits`/`PlanLimitService` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digest, lifecycle emails, admin metrics |

## Domain model
- Conversations are keyed by `EmailThread.conversationKey` (participant-set key, V32), not by RFC threading alone; `/chats/{key}` merges threads.
- `Message.outbound` marks messages sent from the app (V30). `Attachment.blob_ref` is always null (bytes not stored).
- Plan caps (`PlanLimits`): Free 3 mailboxes / 500 threads; Pro 5 mailboxes / unlimited; Business unlimited.

## Auth model
Session-cookie (`JSESSIONID`) + remember-me (30d, persistent token repo) form login (`POST /login`, params `email`,`password`),
optional Google OIDC (only when `GOOGLE_CLIENT_ID` set). **CSRF is on** (exempt: `/h2-console/**`, `/billing/webhook`).
Everything not on the permitAll list in `SecurityConfig` requires authentication. **There is no token (bearer/JWT) auth.**

## Web vs. API
There is currently **no REST/JSON API for a mobile client**. The only JSON-ish endpoints: `POST /chats/refresh`
(`{"imported":n}`), `POST /billing/webhook` (Stripe), SSE `GET /chats/events`. See `API.md`, `WEB.md`.
Controllers delegate to services (`ChatService`, `ConversationListService`, `OutboundMessageService`, ...), so a future
`/api/v1` layer must reuse those services rather than the controllers.

## Background jobs
Feature-flagged schedulers: mailbox polling (`MAILBOX_POLLING_ENABLED`), IMAP IDLE for paid plans (`MAILBOX_IDLE_ENABLED`),
weekly/admin digests, activation, trial-end, re-engagement (default off unless `*.enabled=true`).
New mail publishes `NewMailEvent` -> `ChatEventStream` -> SSE.
