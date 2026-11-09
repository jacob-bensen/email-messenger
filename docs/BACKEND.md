# Backend architecture (as implemented, 2026-11-09)

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf UI,
Spring Data **JPA** (not jOOQ), PostgreSQL (H2 in tests), Flyway (V1–V35).
Package root `com.emailmessenger`.

## Modules
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`), banners, trial nudges |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling, IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService` |
| `web` | `/chats` workspace controllers, mailboxes, pricing/landing, PWA, SEO, SSE (`ChatEventStream`) |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, admin metrics, lifecycle emails |

## Data model (tables)
users, subscriptions, plan_change_events, mail_accounts, email_threads
(owner + `conversation_key`), messages, message_recipients, participants,
attachments (`blob_ref` always null — bytes not stored), saved_searches,
digest_email_preferences, teams/team_members/team_invites, password_reset_tokens,
email_verifications, auth_events, persistent_logins.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30d, JDBC tokens), CSRF **enabled**
(only `/billing/webhook` and `/h2-console/**` exempt). Everything except the
public allow-list in `SecurityConfig` requires authentication. Google OIDC is
enabled only when `GOOGLE_CLIENT_ID` is set. **There is no token/bearer auth.**

## Web vs REST
There is **no REST API**. The only JSON/non-HTML endpoints are
`POST /chats/refresh` (`{"imported":n}`), `GET /chats/events` (SSE),
`POST /billing/webhook` (Stripe) and PWA/SEO assets. Business logic lives
partly in controllers (notably `ConversationController.send`) — see
INTERNAL_TODO.md.
