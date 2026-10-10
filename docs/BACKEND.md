# BACKEND — ConexusMail server

Spring Boot 3 / Java 21 monolith. Serves the Thymeleaf website and (planned) the REST API for the mobile client.
Source of truth for all state and business rules.

## Stack (as implemented)
- Persistence: **Spring Data JPA** (Hibernate, `ddl-auto: validate`) on PostgreSQL 16; H2 in tests. **jOOQ is NOT used** (the generic routine assumes it; this repo does not).
- Migrations: Flyway, `src/main/resources/db/migration`, V1..V35 (immutable once merged).
- Build: Maven wrapper (`./mvnw`). No CI by design (see CLAUDE.md).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` for `/chats`, `MailboxController`, `GmailConnectController`, saved searches, pricing, legal, PWA), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records |
| `email` | IMAP/Gmail integration: import, polling, IDLE, MIME parsing, credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling |
| `billing` | Stripe checkout/portal/webhooks, plan limits (`PlanLimitService`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema overview
Tables: `users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`, `email_threads` (has `conversation_key`, owner, unread), `messages`, `message_recipients`, `attachments` (`blob_ref` always null today), `saved_searches`, `digest_email_preferences`, `teams`, `team_members`, `team_invites`, `auth_events`, `email_verifications`, `password_reset_tokens`. (`thread_notes` dropped in V33.)

## Auth model
- Session cookie (form login or Google OIDC), remember-me (`persistent_logins`), CSRF enabled (ignored only for `/billing/webhook`, `/h2-console/**`).
- Everything except an allow-list in `SecurityConfig` requires authentication.
- **No token/API auth exists.** The mobile client needs one (see INTERNAL_TODO #1).

## Web vs REST
Currently web only. Controllers render Thymeleaf views and delegate to services. The only non-HTML endpoints are `/chats/events` (SSE), PWA assets and `/billing/webhook`. When the REST API is added it MUST call the same services (`ChatService`, `ConversationListService`, `ReplyService`, `MailAccountService`), not re-implement them.
