# BACKEND architecture (ConexusMail / email-messenger)

Classified as BACKEND by evidence: `pom.xml`, Spring annotations, Flyway migrations V1–V35.
Deviation from the generic target stack: persistence is **Spring Data JPA (Hibernate), not jOOQ**; there is no codegen step.

## Stack
Java 21, Spring Boot 3, Thymeleaf, JPA, Flyway, PostgreSQL 16 (H2 in tests), Spring Security (form login + Google OIDC), Stripe, Jakarta Mail (IMAP/SMTP), jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA, SEO), SSE `ChatEventsController`, `GlobalExceptionHandler` |
| `service` | Conversation building: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, bubble/run view records |
| `email` | `EmailImportService`, `MimeMessageParser`, IMAP client/IDLE, `MailboxPollingService/Scheduler`, Gmail OAuth, `CredentialEncryptor` |
| `auth` | `SecurityConfig`, registration, login throttle/audit, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Schema overview (Flyway, immutable once merged; next = V36)
users, subscriptions, mail_accounts (IMAP password or Gmail OAuth, encrypted credentials), email_thread (with `conversation_key`), message, message_recipient, participant, attachment (`blob_ref` always null: bytes not stored), saved_searches, digest_email_preferences, teams/team_members/team_invites, auth_events, password_reset_tokens, email_verifications, plan_change_events.

## Auth model
Session-cookie auth (form login `/login`, Google OIDC at `/auth/google/start`) with CSRF enabled (ignored only for `/h2-console/**`, `/billing/webhook`). Everything except the public allow-list in `SecurityConfig` requires authentication. **No token auth exists yet** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
Only the Thymeleaf site exists. The only JSON/stream endpoints are `POST /chats/refresh` (`{"imported":n}`) and `GET /chats/events` (SSE). A `/api/v1` REST layer is planned in `API.md`; it must call the same services the web controllers use (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`), not duplicate logic.

## Commands
`./mvnw test`, `./mvnw clean package`; see `CLAUDE.md`.
