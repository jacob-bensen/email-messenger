# Backend architecture

Reverse-engineered 2026-10-16 from the code. Anything not verified is marked TBD.

> **Stack reality check.** The routine template assumes jOOQ; this repo uses
> **Spring Data JPA** (Hibernate, `ddl-auto: validate`) + Flyway. There is no jOOQ
> codegen. Do not introduce jOOQ without a decision recorded in INTERNAL_TODO.md.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Security (form login, Google OIDC, remember-me
via `persistent_logins`), Spring Data JPA, PostgreSQL 16 (H2 in tests), Flyway
(`V1`..`V35`), Jakarta Mail (IMAP/SMTP), Stripe Java, jsoup, GreenMail/Testcontainers tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Chat workspace (`ConversationController`, `ChatService`, `ConversationListService`), mailboxes, saved searches, PWA, SEO, legal, pricing, SSE (`ChatEventsController`/`ChatEventStream`) |
| `auth` | Registration, login, Google OIDC, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), billing banners |
| `email` | IMAP polling/IDLE, import (`EmailImportService`, `MimeMessageParser`), Gmail OAuth, credential encryption |
| `service` | Domain services/view records: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `domain`/`repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, weekly digest, lifecycle emails, admin metrics |

## Domain / schema (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts` (encrypted creds, auth type
PASSWORD/OAuth, `last_seen_uid`, `last_seen_sent_uid`, signature), `email_threads`
(owner, unread, `conversation_key`), `messages` (incl. outbound flag), `message_recipients`,
`participants`, `attachments` (`blob_ref` always null today — not downloadable),
`saved_searches`, `teams`/`team_members`/`team_invites`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins`.
A *conversation* is a `conversationKey` derived from the participant set
(`ConversationKeyService`) merging many email threads into one timeline.

## Auth model
Session cookie (JSESSIONID) + CSRF on all POSTs (exempt: `/billing/webhook`, `/h2-console/**`).
Everything is authenticated except the public allow-list in `SecurityConfig`. Login throttled
by `LoginThrottleFilter`. **There is no token/Bearer auth** — a mobile client cannot
authenticate yet (see INTERNAL_TODO.md).

## Web vs REST
There is **no REST/JSON API**. The only `@RestController` is `BillingWebhookController`
(Stripe). Two endpoints return non-HTML: `POST /chats/refresh` (JSON `{imported:n}`) and
`GET /chats/events` (SSE). See `API.md` and `WEB.md`.

## Known logic-in-controller (blocks a shared service layer)
`ConversationController` holds send orchestration: `sendingAccount`, `withSignature`,
recipient parsing, and the `replyService.send*` + `outboundMessageService.record*` pairing.
This must move into a service before any JSON endpoint reuses it.
