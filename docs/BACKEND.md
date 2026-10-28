# Backend architecture

Spring Boot 3.5 / Java 21 / Maven. **Persistence is Spring Data JPA (Hibernate), not jOOQ** — there is no jOOQ codegen. Schema is owned by Flyway (`src/main/resources/db/migration`, V1–V35, immutable once merged). PostgreSQL 16 in prod/local, H2 in tests.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, Gmail connect, pricing, legal, SEO, PWA, saved searches), `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE) |
| `service` | Conversation building and IM transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, bubble/run view records) |
| `email` | IMAP polling + IDLE, MIME parsing, import (`EmailImportService`), Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `auth` | Form login + Google OIDC, registration, email verification, password reset, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial conversion |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, nudges, admin revenue metrics |

## Schema overview
`participants`, `email_threads` (owner, unread, `conversation_key`), `messages` (+ `outbound`), `message_recipients`, `attachments` (`blob_ref` always null today), `users`, `persistent_logins`, `subscriptions`, `mail_accounts` (IMAP/OAuth, polling policy, last-seen UIDs, signature), `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `plan_change_events`, `teams`, `team_members`, `team_invites`.

## Auth model
Session cookie (form login, remember-me via `persistent_logins`) or Google OIDC. CSRF is enabled (ignored only for `/h2-console/**` and `/billing/webhook`). Login throttling via `LoginThrottleFilter`. **There is no token/API-key auth** — a mobile client cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs REST
There is no REST API for a mobile client. The only non-HTML endpoints are `POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook`, and static/SEO/PWA resources. See `docs/API.md`. Web and any future API must share `ChatService` / `ConversationService` / `ReplyService` / `MailAccountService`.
