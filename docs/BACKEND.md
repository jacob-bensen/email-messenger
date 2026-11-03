# Backend architecture

Classified as BACKEND by evidence: `pom.xml`, Spring Boot annotations, Flyway migrations (V1–V35).
Stack as implemented: Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security (form login, Google OIDC, remember-me), Stripe, Jakarta Mail.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `web` | Thymeleaf MVC controllers + view services (`ConversationController`, `ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, SSE `ChatEventStream`) |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send) |
| `email` | Mailbox integration: IMAP polling (`MailboxPollingService`), IMAP IDLE, import (`EmailImportService`), Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `auth` | Registration, login, password reset, email verification, Google OIDC, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |

## Schema overview (Flyway `src/main/resources/db/migration`)
Core: `users`, `subscriptions`, `mail_accounts`, `participants`, `email_threads` (has `conversation_key`, `unread`), `messages` (outbound flag from V30), `message_recipients`, `attachments` (`blob_ref` always null today), `saved_searches`, `teams`/`team_members`/`team_invites`, auth/billing audit tables (`auth_events`, `plan_change_events`, `password_reset_tokens`, `email_verifications`), `persistent_logins`, `digest_email_preferences`. Migrations are immutable; add `V36+`.

## Core concept: conversationKey
A "chat" is the set of non-owner participants; `EmailThread.conversation_key` (hash of the participant set) merges many email threads into one timeline. Messages in a chat come from `EmailThreadRepository.findMessagesByConversationKey[ForMailbox]`.

## Auth model
Session-cookie auth (JSESSIONID + 30-day persistent remember-me), CSRF **enabled** (ignored only for `/h2-console/**`, `/billing/webhook`). Everything not in the `SecurityConfig` permit list requires authentication; controllers resolve the user via `UserService.requireByEmail(principal.getName())`. Admin pages gated by `admin.emails` allowlist (404 otherwise).

## Web vs. REST
There is currently **no REST/JSON API** for a mobile client. All user-facing behavior is Thymeleaf MVC returning views/redirects; the only JSON/stream endpoints are `POST /chats/refresh` and the SSE stream `GET /chats/events`. Business logic already lives in services, but several orchestration steps (recipient parsing, signature appending, sending-account selection, mailbox scoping) live as private methods in `ConversationController` and must be extracted into a service before a REST controller can share them. See `../INTERNAL_TODO.md`.
