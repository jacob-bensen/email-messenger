# BACKEND

Spring Boot 3 / Java 21 monolith ("ConexusMail", repo `email-messenger`). Server-rendered
Thymeleaf UI; no separate JSON API exists yet (see `API.md`).

> Reality vs. routine template: persistence is **Spring Data JPA + Flyway + PostgreSQL**
> (H2 in tests), not jOOQ. There is no jOOQ codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/pricing/legal/PWA/SEO), SSE `ChatEventStream` |
| `service` | IM transform, `ConversationService`, `ReplyService`, `EmailBodyCleaner`, conversation keys |
| `email` | IMAP/Gmail import, polling (`MailboxPollingService`), IDLE, SMTP credentials, MIME parsing |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | form login, Google OIDC, remember-me, email verification, password reset, throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `admin`, `activation`, `reengagement` | teams/invites, digests, operator metrics, lifecycle email |

## Schema (Flyway `src/main/resources/db/migration`, V1..V35)
users, mail_accounts, participants, email_threads, messages, message_recipients, attachments
(`blob_ref` always null), saved_searches, thread_notes, subscriptions, plan_change_events,
teams, team_members, team_invites, email_verifications, password_reset_tokens,
persistent_logins, auth_events, digest_email_preferences. Migrations are immutable.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`/`password`),
optional Google OIDC, remember-me (30d). CSRF is ON except `/h2-console/**` and
`/billing/webhook`. Everything not on the permitAll list in `SecurityConfig` requires auth.
No token/bearer auth exists — a mobile client cannot authenticate non-interactively yet.

## Web vs REST
Only JSON-ish endpoints today: `POST /chats/refresh` (`{"imported":n}`), SSE `GET /chats/events`,
`POST /billing/webhook`. Business logic lives in services (`ConversationService`, `ReplyService`,
`MailAccountService`, `BillingService`); controllers are thin.

## Related
Sibling repo `conexus-mail-mobile` (Flutter client; currently empty). Operating notes for the
earlier autonomous routine live in `master/` (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS).
