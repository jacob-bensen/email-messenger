# BACKEND.md — architecture (reverse-engineered 2026-10-11)

Spring Boot 3 / Java 21 monolith, **Spring Data JPA** (not jOOQ — the generic
routine's jOOQ assumption does not hold here), PostgreSQL 16 (H2 in tests),
Flyway (V1–V35 in `src/main/resources/db/migration`), Thymeleaf server-rendered
UI. Base package `com.emailmessenger`.

## Modules (packages)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/PWA/SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation view models, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` |
| `email` | IMAP/Gmail import, `MimeMessageParser`, polling + IDLE, `MailAccountService`, credential encryption |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Auth model
Session cookie (form login or Google OIDC), CSRF enabled (ignored only for
`/h2-console/**` and `/billing/webhook`). Everything not on the public allow-list
in `SecurityConfig` requires authentication. **There is no token/API auth yet.**

## Web vs REST
No REST API exists. The only JSON/stream endpoints are `POST /chats/refresh`
(`{"imported": n}`) and the SSE stream `GET /chats/events`. See `API.md`.
Controllers delegate to services (`ChatService`, `ReplyService`, ...), which is
the layer a future `/api/v1` must reuse.

## Schema overview
users, subscriptions, plan_change_events, mail_accounts (IMAP/OAuth, polling
policy, last-seen UIDs, signature), email_thread (owner, unread, conversation_key),
message (+outbound flag), participant, message_recipient, attachment (`blob_ref`
always null today), saved_searches, digest_email_preferences, teams/invites,
auth_events, password/email-verification tokens.
