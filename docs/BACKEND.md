# Backend architecture

Spring Boot 3 / Java 21 app ("ConexusMail") that renders email as a texting-style
conversation UI. Persistence is Spring Data **JPA** (Hibernate, `ddl-auto: validate`)
on PostgreSQL 16 (H2 in tests), schema owned by **Flyway** (`src/main/resources/db/migration`,
V1–V35; immutable once merged). *The sync-routine template assumes jOOQ; this repo does
not use it — there is no codegen step.*

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: `/chats` workspace (`ConversationController`), mailboxes, saved searches, pricing/landing/legal/SEO/PWA, `ChatEventsController` (SSE) |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | IMAP/Gmail-OAuth import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), chat/conversation view models, `ConversationKeyService`, `ReplyService` |
| `domain` / `repository` | JPA entities and Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics (`/admin/**`, allow-listed emails) |

## Data model (tables)

`users`, `subscriptions`, `plan_change_events`, `mail_accounts` (per-user mailbox; `auth_type`
password|oauth, polling cursor `last_seen_uid`, `last_seen_sent_uid`, signature),
`email_threads` (owner-scoped; `conversation_key` = participant-set key used by `/chats/{key}`),
`messages` (incl. outbound flag, V30), `message_recipients`, `participants`, `attachments`
(`blob_ref` always null today — bytes are not stored), `saved_searches`, `teams`,
`team_members`, `team_invites`, `digest_email_preferences`, `auth_events`,
`password_reset_tokens`, `email_verifications`, `persistent_logins` (remember-me).

## Auth model

Session-cookie auth (`JSESSIONID`) via form login (`email`/`password` at `POST /login`) or
Google OIDC (`/auth/google/start`); 30-day persistent remember-me; CSRF **enabled** (only
`/billing/webhook` and `/h2-console/**` exempt). Everything not on the `SecurityConfig`
permit-list requires authentication. **There is no token/bearer auth** — a mobile client
cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs REST

The app is a server-rendered website. The only JSON/stream endpoints are
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE) and `POST /billing/webhook`
(Stripe). **No REST API for the mobile client exists yet**; `docs/API.md` records the
current surface and the gaps. Controllers resolve the user via `UserService.requireByEmail`
and delegate to services; some mailbox-scope selection helpers (`selectMailbox`,
`scopeAddress`, `chatFor`) still live in `ConversationController` and must move to a
service before an API can share them.
