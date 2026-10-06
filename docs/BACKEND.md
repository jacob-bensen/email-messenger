# BACKEND.md — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Classification evidence: `pom.xml`,
`@Controller`s, Flyway migrations `V1..V35`. **Deviation from the generic target
stack:** persistence is Spring Data JPA (`domain/` entities + `repository/`), *not*
jOOQ; there is no jOOQ codegen step. Build is Maven; Postgres 16 in prod, H2 in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers: chats workspace, mailboxes, Gmail connect, saved searches, SEO, PWA, legal, pricing/landing/demo, SSE (`/chats/events`) |
| `auth/` | Security config, form login + Google OIDC, registration, password reset, email verification, account settings, login throttling, auth audit events |
| `billing/` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial conversion nudges |
| `email/` | IMAP client + IDLE, polling scheduler/service, MIME parsing, Gmail OAuth, credential encryption, import |
| `service/` | IM transform (`IMTransformService`, `EmailBodyCleaner`), chat/conversation view models, `ReplyService`, conversation keys |
| `domain/`, `repository/` | JPA entities / Spring Data repos |
| `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` | Teams + invites, weekly digest, lifecycle emails, operator metrics/win-back |

## Data model (Flyway `src/main/resources/db/migration`, immutable)
users, subscriptions, mail_accounts (IMAP password or Gmail OAuth; last-seen UID cursors),
email_thread (owner, `conversation_key`, unread), message / message_recipient /
participant / attachment (`blob_ref` always null today), saved_searches, teams /
team_members / team_invites, digest_email_preferences, password_reset / email
verification tokens, auth_events, plan_change_events, persistent_logins (remember-me).

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`/`password`) or Google
OIDC; 30-day remember-me; CSRF **enabled** (exempt: `/billing/webhook`, `/h2-console/**`).
Everything not on the permit-all list in `auth/SecurityConfig` requires authentication.
**There is no token/bearer auth, so a mobile client cannot authenticate yet** (see
INTERNAL_TODO).

## Web vs. REST
There is no REST API. Controllers return Thymeleaf views and are thin over services
(`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`,
`MailboxPollingService`, ...). Only JSON endpoints: `POST /chats/refresh`, SSE
`/chats/events`, Stripe webhook. A future `/api/v1` must reuse these services. See `API.md`.

## Live delivery
`MailboxPollingService` / `ImapIdleService` publish `NewMailEvent` → `ChatEventStream`
→ SSE event `new-mail` with `{"imported":N}`.
