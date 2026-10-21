# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Reverse-engineered 2026-10-21 from the code.
Persistence is **Spring Data JPA + Flyway (PostgreSQL; H2 in tests)** — there is no jOOQ.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: `/chats` workspace, mailboxes, saved searches, pricing/landing/legal/PWA/SEO, `ChatEventStream` (SSE) |
| `auth` | Spring Security config, form + Google OIDC login, registration, password reset, email verification, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits, subscription lifecycle |
| `service` | Conversation building: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, view records (`ChatConversation`, `ConversationListItem`, ...) |
| `email` | IMAP/SMTP, polling (`MailboxPollingService`), IMAP IDLE, Gmail OAuth, import |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digest email, lifecycle emails, admin metrics |

## Schema overview (Flyway `V1`..`V35`)
Users/subscriptions (V2, V4, V17, V23, V26-28), mail accounts (V5-7, V29, V31, V35), threads/messages/participants/attachments (V1, V3, V9, V30, V32), saved searches (V10-11), digests (V12), auth/security (V14-16, V18-19), teams (V24), user mail prefs (V34). Migrations are immutable; add `V36+`.
Conversations are keyed by `email_thread.conversation_key` (participant-set hash, V32).

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days), CSRF **on** for every POST (exempt: `/h2-console/**`, `/billing/webhook`). Login = form (`POST /login`, fields `email`,`password`) or Google OIDC. Everything not in the permit-list in `SecurityConfig` requires authentication. **No token/bearer auth exists** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
There is no REST API. Controllers return Thymeleaf views; only exceptions:
`POST /chats/refresh` (JSON `{"imported":n}`), `GET /chats/events` (SSE), `POST /billing/webhook`.
Business logic lives in services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailboxPollingService`, ...); controllers still do some assembly (mailbox scoping, filter parsing, attachment validation in `ConversationController`) that a JSON API would need to share — extract before duplicating.
See `API.md` (contract, current + planned) and `WEB.md` (pages).
