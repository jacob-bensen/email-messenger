# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-21. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ), Flyway, PostgreSQL 16 (H2 in tests),
Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP). Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace), SSE (`ChatEventsController`), PWA/SEO, mailbox + saved-search controllers, `ChatService`, `OutboundMessageService` |
| `service` | Conversation building / IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP/Gmail import and polling (`EmailImportService`, `MailboxPollingService`, `ImapIdleService`, `MailAccountService`, `CredentialEncryptor`) |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `admin`, `activation`, `reengagement`, `digest`, `team` | Operator metrics, lifecycle emails, weekly digests, teams/invites |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Schema overview (Flyway, `db/migration`, V1..V35, immutable)
users, subscriptions, plan_change_events, persistent_logins, mail_accounts, participants,
email_threads, messages, message_recipients, attachments (`blob_ref` always null today),
saved_searches, digest_email_preferences, password_reset_tokens, email_verifications,
auth_events, teams, team_members, team_invites, thread_notes.
Conversations are keyed by a participant-set `conversationKey` (not a table; derived).

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`,
optional `remember-me`) or Google OIDC (only when `GOOGLE_CLIENT_ID` set). Remember-me tokens
persisted in `persistent_logins`. CSRF is ON (exempt: `/h2-console/**`, `/billing/webhook`).
Everything not in the `permitAll` list in `SecurityConfig` requires authentication.
**There is no token/bearer auth today** — see `INTERNAL_TODO.md`.

## Web vs REST
The app is server-rendered; controllers return Thymeleaf views. The only JSON/stream endpoints are
`POST /chats/refresh` (JSON) and `GET /chats/events` (SSE). There is **no REST API for the mobile
client yet**. When one is added it must call the same services as the web controllers
(`ChatService`, `ConversationService`, `OutboundMessageService`, `MailAccountService`).
Note: `ConversationController` currently holds some orchestration (mailbox selection,
`scopeAddress`, `populateList`) that a REST controller would need — extract to a service first.
