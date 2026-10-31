# BACKEND — ConexusMail (email-messenger)

Reverse-engineered from the code on 2026-10-31. Items not verifiable from code are marked TBD.

## Stack
Java 21, Spring Boot 3, **Spring Data JPA (Hibernate)** — NOT jOOQ, so there is no codegen step —
PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf,
Spring Security, Stripe (stripe-java), Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), SSE `ChatEventStream` |
| `service` | Conversation/IM transform: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, view records |
| `email` | IMAP/Gmail integration: import, polling (`MailboxPollingService`), IDLE, OAuth, credential encryption |
| `auth` | Form login + Google OIDC, registration, verification, password reset, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (Free/Pro/Business), trial nudges |
| `team` | Team invites and membership |
| `admin`, `activation`, `digest`, `reengagement` | Metrics, lifecycle emails, schedulers |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Schema overview
Tables: users, persistent_logins, subscriptions, plan_change_events, mail_accounts, participants,
email_threads (has `conversation_key`), messages, message_recipients, attachments (`blob_ref` always null),
saved_searches, digest_email_preferences, password_reset_tokens, email_verifications, auth_events,
teams, team_members, team_invites. (`thread_notes` was dropped in V33.)

## Auth model
Session cookie (JSESSIONID) + remember-me (30 days) via form login at `POST /login` (params `email`, `password`),
plus Google OIDC. **CSRF is enabled** for everything except `/h2-console/**` and `/billing/webhook`.
Everything not in the permit-all list (see WEB.md) requires an authenticated session.
There is **no token/Bearer auth** and **no JSON API for a mobile client** (see API.md).

## Web vs. REST
Only three machine-facing endpoints exist: `POST /chats/refresh` (JSON), `GET /chats/events` (SSE),
`POST /billing/webhook`. All other routes render Thymeleaf or redirect. Business logic lives in services;
controllers call services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`, ...). A future mobile API must reuse these same services.
