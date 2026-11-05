# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Persistence is **Spring Data JPA + Flyway**
(not jOOQ), PostgreSQL in prod, H2 in tests. Package root `com.emailmessenger`.

## Modules
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial/win-back schedulers |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling, IMAP IDLE, Gmail OAuth, credential encryption |
| `service` | Conversation building: `ConversationService`, `ChatConversation`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, marketing, PWA, SEO) and SSE (`/chats/events`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema overview (Flyway V1..V35)
users, subscriptions, plan_change_events, mail_accounts (IMAP password or Gmail OAuth; last-seen UID cursors),
email_threads (owner, unread, `conversation_key` = participant set), messages (+`outbound`), message_recipients,
participants, attachments (`blob_ref` always null today), saved_searches, digest_email_preferences,
teams/team_members/team_invites, password_reset_tokens, email_verifications, auth_events, persistent_logins.
Migrations are immutable; add a new `V{n}`.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 d), CSRF **enabled** (exempt: `/billing/webhook`, `/h2-console/**`).
Everything not in the permit-list in `SecurityConfig` requires login. Login is form POST `/login` (`email`,`password`);
Google OIDC only when `GOOGLE_CLIENT_ID` is set. Login throttling via `LoginThrottleFilter`.
**There is no token (Bearer/JWT) auth** — see INTERNAL_TODO.

## Web vs REST
All UI is server-rendered Thymeleaf returning HTML/redirects. The only JSON/non-HTML handlers are
`POST /chats/refresh` (`{"imported":n}`), `GET /chats/events` (SSE) and `POST /billing/webhook` (Stripe).
**No mobile REST API exists yet**; the planned one is specified in `docs/API.md` §Planned.
Rule: any future `/api/v1` controller must call the same services as the web controllers
(`ChatService`, `ConversationListService`, `ReplyService`/`OutboundMessageService`, `MailAccountService`).
