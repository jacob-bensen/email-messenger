# ConexusMail backend — architecture

Reverse-engineered 2026-11-06 from the code. Anything not verified is marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, Spring Data **JPA** (not jOOQ — the
routine's generic target stack differs; see INTERNAL_TODO), PostgreSQL 16 (H2 in
tests), Flyway (`src/main/resources/db/migration`, V1..V35), Spring Security, Stripe,
Jakarta Mail (IMAP/SMTP), Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/PWA/SEO), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | Mailbox connect, IMAP polling/IDLE, Gmail OAuth, MIME parsing, import (`EmailImportService`), credential encryption |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`), `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, operator metrics (`/admin/**`) |

## Data model (tables, from migrations)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`,
`email_threads`, `messages`, `message_recipients`, `attachments` (`blob_ref` always
null today), `thread_notes`, `saved_searches`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins`,
`teams`, `team_members`, `team_invites`.
A *conversation* is keyed by `conversationKey` (hash of the participant set) and
merges several `email_threads`.

## Auth model
- Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,
  `password`) or Google OIDC; optional remember-me cookie (30 d).
- CSRF is **enabled** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not in the `permitAll` list in `auth/SecurityConfig` requires login.
- `/admin/**` is gated by `AdminAuthorizer` (allowlist, 404 for others).
- **There is no token/bearer auth and no JSON API yet** — a mobile client cannot
  authenticate today (see `docs/API.md`).

## Web pages vs API
All current endpoints are server-rendered MVC routes that delegate to services.
Only JSON-ish endpoints: `POST /chats/refresh` (`{"imported":n}`), SSE
`GET /chats/events`, `POST /billing/webhook` (Stripe). Business logic lives in
services (`ChatService`, `ReplyService`, `MailAccountService`, …); controllers do
param parsing + model population. A future REST layer must call the same services.
