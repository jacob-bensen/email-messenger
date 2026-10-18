# BACKEND.md — ConexusMail backend architecture

Reverse-engineered from the code on 2026-10-18. Unknowns are marked TBD.

## Classification
This repo (`email-messenger`) is the **BACKEND**: `pom.xml`, Spring Boot 3, Flyway
migrations (`V1`–`V35`), Thymeleaf templates. The sibling `conexus-mail-mobile`
(only a README at time of writing) is the **FRONTEND** (Flutter).

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (not jOOQ — no codegen step),
PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security, Stripe Java,
Jakarta Mail (IMAP/SMTP), jsoup, GreenMail + Testcontainers in tests. Maven wrapper.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | form login, Google OIDC, registration, email verification, password reset, account settings, login throttle, remember-me |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 threads, PRO 5/∞, BUSINESS ∞), trial nudges |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (paid tiers), Gmail OAuth, credential encryption |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…) |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/SEO/PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `team` | team invites |
| `digest`, `activation`, `reengagement` | scheduled lifecycle emails |
| `admin` | revenue / funnel / churn metrics (`/admin/revenue`) |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Schema overview (Flyway `src/main/resources/db/migration`)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`,
`email_threads` (has `conversation_key`, owner, unread), `messages`
(has outbound flag), `message_recipients`, `participants`, `attachments`
(`blob_ref` always null — bytes not stored), `saved_searches`,
`digest_email_preferences`, `teams`, `team_members`, `team_invites`,
`password_reset_tokens`, `email_verifications`, `auth_events`,
`persistent_logins` (remember-me). `thread_notes` was dropped in V33.
Migrations are immutable once merged.

Core concept: a **conversation** is keyed by `conversationKey` = hash of the
participant set; it merges many `email_threads` into one texting-style timeline.

## Auth model
Session-based (JSESSIONID cookie) with CSRF enabled; form login (`email`/`password`),
optional Google OIDC (only when `GOOGLE_CLIENT_ID` set), 30-day persistent remember-me,
login throttling filter. Everything not whitelisted in `SecurityConfig` requires
authentication. **There is no token/Bearer auth** — a mobile client cannot
authenticate today (see INTERNAL_TODO).

## Web vs REST
All user-facing functionality is Thymeleaf (server-rendered, form POST + redirect).
The only JSON/machine endpoints: `POST /billing/webhook`, `POST /chats/refresh`,
SSE `GET /chats/events`, `/actuator/health`. **No REST API for the mobile client
exists yet.** Note that `ConversationController` holds business logic itself
(signature appending, reply-vs-new-thread choice, recipient derivation) that must
move into a service before a REST controller can share it.

## Live delivery
`MailboxPollingService` imports mail and publishes `NewMailEvent`; `ChatEventStream`
fans out to per-user SSE emitters. `ImapIdleService` for paid accounts.

## Tests
`src/test/java/com/emailmessenger/**` mirrors main packages; includes integration
tests with GreenMail. Run `./mvnw test`.
