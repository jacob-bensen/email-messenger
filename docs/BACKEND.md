# BACKEND — architecture

Reverse-engineered 2026-10-06 from the code. Unknowns are marked TBD.

> Note: the sync-routine template assumes jOOQ. This repo uses **Spring Data
> JPA / Hibernate** (`ddl-auto: validate`) + Flyway. There is no jOOQ codegen.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Data JPA, Flyway (35 migrations,
`src/main/resources/db/migration`), PostgreSQL 16 (H2 in tests), Spring
Security (form login + Google OIDC + remember-me), Stripe, Jakarta Mail
(IMAP/SMTP), SSE for live updates. Build: Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, landing, legal, SEO, PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `service` | Conversation view models, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP/Gmail import (`EmailImportService`, `MimeMessageParser`), polling + IDLE, credential encryption, `MailAccountService` |
| `auth` | Security config, registration, password reset, email verification, Google OIDC, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited) |
| `team` | Teams + invites |
| `digest`, `activation`, `reengagement`, `admin` | Schedulers, email nudges, admin revenue/funnel metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema overview
`users`, `subscriptions`, `plan_change_events`, `auth_events`,
`persistent_logins`, `email_verifications`, `password_reset_tokens`,
`mail_accounts` (encrypted IMAP/SMTP or Gmail OAuth creds, per-account polling
policy), `email_threads`, `messages`, `message_recipients`, `participants`,
`attachments` (`blob_ref` always null — bytes are not stored), `thread_notes`,
`saved_searches`, `teams`, `team_members`, `team_invites`,
`digest_email_preferences`. Conversations are keyed by a participant-set
`conversationKey`.

## Auth model
Session cookie (`JSESSIONID`) + CSRF token on all POSTs (only `/billing/webhook`
and `/h2-console/**` are exempt) + optional remember-me (30 days, DB-backed).
Form login: `POST /login` (`email`, `password`). Google OIDC when
`GOOGLE_CLIENT_ID/SECRET` are set. **There is no token (JWT/API-key) auth** —
required before a mobile client can talk to the server (see INTERNAL_TODO).

## Web vs. REST
There is **no REST/JSON API**. All functionality is server-rendered HTML;
the only non-HTML endpoints are `/chats/events` (SSE), `/billing/webhook`,
`/chats/{key}/pane` + `/chats/refresh` (HTML fragments), PWA/SEO assets.
Controllers delegate to services (`ChatService`, `OutboundMessageService`,
`MailAccountService`, `BillingService`, …), which is the layer a future REST
controller must reuse.
