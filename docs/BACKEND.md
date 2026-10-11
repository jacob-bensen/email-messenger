# BACKEND.md — architecture

Reverse-engineered 2026-10-11 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (not jOOQ), Flyway (V1–V35),
PostgreSQL 16 (H2 in tests), Thymeleaf, Spring Security (form login, Google OIDC,
remember-me), Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP), jsoup. Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, PWA, SEO, legal), `ChatService`, `ConversationListService`, `ChatEventStream` (SSE), `GlobalExceptionHandler` |
| `auth` | Security config, registration, login throttle, password reset, email verification, Google OIDC, account settings |
| `billing` | Free/Pro/Business plans, `PlanLimits`, Stripe checkout/portal/webhook, trial/win-back |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams+invites, digest mail, lifecycle emails (schedulers off unless flagged), admin metrics |

## Data model (tables)
users, subscriptions, plan_change_events, mail_accounts, participants, email_threads
(owner, `conversation_key`), messages (outbound flag), message_recipients, attachments
(`blob_ref` always null today), saved_searches, digest_email_preferences,
password_reset_tokens, email_verifications, auth_events, teams, team_members,
team_invites, persistent_logins. Migrations are immutable; add new `V{n}`.

## Auth model
Session cookie (JSESSIONID) + remember-me (30d, JDBC token store). CSRF **on**
(exempt: `/h2-console/**`, `/billing/webhook`). Public paths are whitelisted in
`SecurityConfig`; everything else requires auth. `/admin/**` is gated by an email
allowlist (`admin.emails`), 404 otherwise. **There is no token/API-key auth** — a
mobile client cannot authenticate today except by emulating browser form login + CSRF.

## Web vs. REST
There is **no REST API**. All UI is server-rendered. The only machine-readable
endpoints are `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) and the Stripe
webhook. See `API.md`, `WEB.md`.

## Business-logic placement (audit)
`ConversationController` (web) holds non-trivial orchestration that a JSON API would
need to reuse: mailbox selection/scoping (`selectMailbox`, `scopeAddress`),
send-as-new vs. reply branching, signature appending, recipient derivation,
attachment conversion. Extract into a service before adding API endpoints (see
`INTERNAL_TODO.md`).
