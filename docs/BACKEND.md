# BACKEND.md — architecture (reverse-engineered 2026-10-06)

Classification: this repo is the **BACKEND** (Spring Boot, Flyway migrations, Thymeleaf
site). Sibling: `conexus-mail-mobile` (Flutter client; currently an empty scaffold).

## Deviations from the generic target stack
- Persistence is **Spring Data JPA/Hibernate**, not jOOQ. There is no jOOQ codegen.
- Maven build (`./mvnw`), Java 21, Spring Boot 3, PostgreSQL 16 (H2 in tests), Flyway (35 migrations, V1–V35).
- There is **no REST API for a mobile client yet** (see `API.md`). The only JSON/SSE endpoints
  are the three listed there, all session-cookie authenticated.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA, SEO), SSE `ChatEventStream` |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, `EmailImportService`, `CredentialEncryptor` |
| `auth` | Spring Security config, form login + Google OIDC, registration, verification, password reset, `/account*` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), banners, trial conversion |
| `team` | Team invites and membership |
| `digest`, `activation`, `reengagement` | Scheduled emails |
| `admin` | `/admin/revenue` operator metrics |
| `domain`, `repository` | JPA entities, Spring Data repositories |

## Data model (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads`, `messages`,
`message_recipients`, `participants`, `attachments`, `saved_searches`, `teams`, `team_members`,
`team_invites`, `thread_notes`, `digest_email_preferences`, `password_reset_tokens`,
`email_verifications`, `auth_events`, `persistent_logins`. Conversations are keyed by a
participant-set `conversationKey` (`ConversationKeyService`). Attachments store metadata only
(`blob_ref` is always null today).

## Auth model
Spring Security, **session cookie + CSRF** (CSRF ignored only for `/h2-console/**` and
`/billing/webhook`). Form login at `/login` (remember-me via `persistent_logins`), optional
Google OIDC (`/auth/google/start`). Everything outside the permit-all list in `SecurityConfig`
requires authentication. Admin pages use `AdminAuthorizer`. **No token/bearer auth exists.**

## Web vs. API
All business logic lives in services; controllers are thin and return Thymeleaf views. Since no
REST layer exists, there is currently no duplicated web/API logic to audit. A future
`/api/v1` layer must call the same services (`ChatService`, `ConversationListService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`, `PlanLimitService`).

## Commands
`./mvnw test` (794 tests, 1 Docker-only skip as of 2026-10-06); see `CLAUDE.md`.
