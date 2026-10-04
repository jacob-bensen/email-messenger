# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-04. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`; **no jOOQ**),
PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security (+ OAuth2 client for Google),
Jakarta Mail (IMAP/SMTP), Stripe (`stripe-java`), jsoup. Maven wrapper (`./mvnw`).
Product name: ConexusMail (artifact `email-messenger`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail connect, saved searches, SEO, PWA shell, legal, demo, SSE stream |
| `service` | Chat/IM logic: `ConversationListService`/`ConversationService`, `ChatService`, `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, ...) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail integration: import, polling, IDLE, MIME parsing, credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plans, plan limits, trial nudges |
| `admin` | `/admin/revenue` metrics, win-back outreach, weekly operator digest |
| `team`, `digest`, `activation`, `reengagement` | Teams/invites, weekly digest, lifecycle emails |

## Data model (Flyway V1..V35; next migration is **V36**)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`,
`email_threads` (has `conversation_key`, the participant-set key), `messages`,
`message_recipients`, `attachments` (`blob_ref` always null today), `saved_searches`,
`teams`, `team_members`, `team_invites`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins`.
Migrations are immutable once merged.

## Auth model
- Session-cookie form login (`/login`, params `email`, `password`), optional remember-me
  (persistent token table, 30 days), Google OIDC (only when `GOOGLE_CLIENT_ID` is set).
- CSRF is **on** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the permit-list in `SecurityConfig` requires authentication.
- Login throttling via `LoginThrottleFilter`. Admin = emails in `admin.emails`.
- **No token/bearer auth exists** (needed for a mobile client — see INTERNAL_TODO).

## Web vs REST
There is **no REST/JSON API**. The only non-HTML endpoints are the SSE stream
`/chats/events`, the Stripe webhook, `manifest.webmanifest`/`sw.js`, robots/sitemap/icons.
Controllers delegate to services; a future `/api/v1` layer must reuse the same services
(`ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`) rather than the controllers.

## Background work
`MailboxPollingScheduler` (flag `mailbox.polling.enabled`), `ImapIdleService` (flag
`mailbox.idle.enabled`, paid tiers), digest/activation/re-engagement/trial/admin schedulers.
New mail is published as `NewMailEvent` to `ChatEventStream` (SSE).

## Config (env)
`JDBC_DATABASE_URL/USERNAME/PASSWORD`, `MAIL_*`, `GOOGLE_CLIENT_ID/SECRET`,
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `MAILBOX_ENCRYPTION_PASSWORD/SALT`,
`MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`.
