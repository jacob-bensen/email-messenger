# BACKEND

Reverse-engineered 2026-10-27 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`; the
target-stack brief mentions jOOQ, but this repo does not use it), PostgreSQL 16
(H2 in tests), Flyway (`V1`–`V35`), Thymeleaf, Spring Security, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, landing/legal/PWA/SEO), SSE stream, model advices |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits, trial nudges |
| `email` | IMAP/Gmail import, polling + IDLE, MIME parsing, credential encryption |
| `service` | Conversation building, IM transform, body cleaning, reply sending |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest mail, lifecycle emails, operator dashboards |

## Schema overview (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads`
(has `conversation_key`), `messages`, `message_recipients`, `participants`,
`attachments` (`blob_ref` always null today), `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`,
`auth_events`, `teams`, `team_members`, `team_invites`. Migrations are immutable.

## Auth model
Session-cookie auth (form login, remember-me, optional Google OIDC when
`GOOGLE_CLIENT_ID/SECRET` set). CSRF enabled except `/h2-console/**` and
`/billing/webhook`. Everything not in the `permitAll` list in `SecurityConfig`
requires authentication. `/admin/**` is gated by the `admin.emails` allowlist
(non-admin gets 404). **There is no token/bearer auth — a mobile client cannot
authenticate today** (see INTERNAL_TODO).

## Web vs. REST
The backend has **no REST/JSON API**. All user features are server-rendered
Thymeleaf. JSON/stream endpoints that exist: `POST /chats/refresh` (JSON),
`GET /chats/events` (SSE), `POST /billing/webhook` (Stripe). See `API.md`,
`WEB.md`. Controllers delegate to services (`ChatService`,
`ConversationListService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`, `BillingService`, …), which is the layer a future API must reuse.

## Live delivery
`MailboxPollingService` imports mail and publishes `NewMailEvent(userId, importedCount)`;
`ChatEventStream` fans it out to SSE subscribers; `ImapIdleService` holds IDLE
connections for paid tiers.
