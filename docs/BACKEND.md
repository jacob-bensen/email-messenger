# BACKEND.md — architecture (reverse-engineered 2026-10-12)

Spring Boot 3 / Java 21 monolith ("ConexusMail", artifact `email-messenger`).
Server-rendered Thymeleaf website + background workers. **There is no REST/JSON
API for the mobile client yet** — see `docs/API.md`.

## Stack (actual)
Spring Web, Thymeleaf, Spring Data **JPA/Hibernate** (not jOOQ — no codegen step),
Spring Security (+ OAuth2 client for Google), Validation, Mail, Actuator,
Flyway (V1..V34), PostgreSQL 16 (H2 in tests), Jakarta Mail (IMAP), jsoup,
Stripe Java. Tests: JUnit 5, Spring Boot Test, Testcontainers, GreenMail.
Config via env vars (`JDBC_DATABASE_*`, `STRIPE_*`, `GOOGLE_CLIENT_*`,
`MAILBOX_ENCRYPTION_*`, `MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, Gmail connect, saved searches, pricing/legal/SEO/PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Domain services/view-model records: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP), `ChatConversation`/`ConversationListItem`/`BubbleRun` records |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE, Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Form login, Google OIDC, registration, verify-email, password reset, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digest, lifecycle emails, operator metrics |

## Data model (tables)
users, persistent_logins, subscriptions, plan_change_events, mail_accounts,
participants, email_threads (owner, unread, `conversation_key`), messages
(outbound flag), message_recipients, attachments (`blob_ref` always null today),
saved_searches, digest_email_preferences, password_reset_tokens,
email_verifications, auth_events, teams, team_members, team_invites.
Plans: `FREE`, `PRO`, `BUSINESS`.

## Core concept
A **conversation** = all messages sharing a participant set, identified by
`conversationKey` (string, produced by `ConversationKeyService`), merged across
email threads. Chat list = `ConversationListItem`, open chat = `ChatConversation`.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (persistent token, 30d), CSRF **on**
for everything except `/billing/webhook` and `/h2-console/**`. Form login at
`POST /login` (`email`,`password`), Google OIDC optional. Everything not in the
permit-list in `auth/SecurityConfig` requires authentication. No bearer/JWT
mechanism exists.

## Web vs. API
All current controllers return Thymeleaf views (redirect-after-POST); only
`POST /chats/refresh` (JSON `{"imported":n}`) and `GET /chats/events` (SSE)
are machine-oriented. Business logic lives in services, but
`ConversationController` still holds orchestration (recipient parsing, sender
selection, signature append, attachment conversion) that a JSON API would have
to duplicate — see INTERNAL_TODO #2.
