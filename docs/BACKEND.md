# BACKEND — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail", package `com.emailmessenger`). Server-rendered
Thymeleaf UI + JPA (Hibernate) persistence on PostgreSQL 16 (H2 in tests), Flyway migrations
(`src/main/resources/db/migration`, V1–V35, immutable once merged). **There is no jOOQ** — the
routine's target stack assumed it; this repo uses Spring Data JPA entities in `domain/` and
repositories in `repository/`.

## Modules (packages)
| Package | Role |
|---|---|
| `auth` | Form login (`/login`, email+password, BCrypt), remember-me (30 d, `persistent_logins`), Google OIDC (optional, gated on `GOOGLE_CLIENT_ID`), registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe Checkout/Portal/Webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/win-back nudges. Plans: Free / Pro / Business |
| `email` | Mailbox connect (IMAP password + Gmail OAuth), import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE (paid tiers), credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models (`ChatConversation`, `ConversationListItem`, `BubbleMessage`), `ReplyService`, `ConversationKeyService` |
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, legal, PWA, SEO), SSE `ChatEventStream` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema overview (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`, `email_threads`
(has `conversation_key` = participant-set key, V32), `messages` (`outbound` flag V30),
`message_recipients`, `attachments` (`blob_ref` always null today), `saved_searches`,
`digest_email_preferences`, `teams`, `team_members`, `team_invites`, `auth_events`,
`password_reset_tokens`, `email_verifications`, `persistent_logins`.

## Auth model
Session-cookie (JSESSIONID) + CSRF (enabled; ignored only for `/h2-console/**` and
`/billing/webhook`). Everything except the public allow-list in `SecurityConfig` requires
authentication. Controllers resolve the user via `Principal.getName()` (the email) →
`UserService.requireByEmail`. **No token/bearer auth exists** — a mobile client currently has
no supported way to authenticate (see `INTERNAL_TODO.md`).

## Web vs REST
All user-facing behavior is Thymeleaf HTML (see `WEB.md`). The only JSON/non-HTML endpoints:
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook` (Stripe).
There is no `/api/**` REST layer yet; `docs/API.md` records this and a draft contract.
Business logic lives in services (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`ReplyService`, …) which a future REST controller must reuse.

## Live delivery
`MailboxPollingScheduler` (default 5 min) + `ImapIdleService` push `NewMailEvent` →
`ChatEventStream` (SSE, keepalive 25 s). Web Push is a roadmap item.
