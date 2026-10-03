# BACKEND — architecture (ConexusMail / email-messenger)

Reverse-engineered from the code on 2026-10-03. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, **Spring Data JPA/Hibernate** (not jOOQ — there is no
codegen step), PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1–V35),
Spring Security (form login + Google OIDC + persistent remember-me), Stripe (`stripe-java`),
Jakarta Mail (IMAP/SMTP), jsoup, GreenMail + Testcontainers in tests. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers for the product UI (`/chats`, `/mailboxes`, `/searches`, pricing/landing, legal, PWA, SEO), SSE stream, view services (`ConversationListService`, `ChatService`, `OutboundMessageService`) |
| `service` | Mail → chat transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…) |
| `email` | Mailbox integration: IMAP client, Gmail OAuth, polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), MIME parsing, import (`EmailImportService`), credential encryption |
| `domain` / `repository` | JPA entities and Spring Data repositories |
| `auth` | Security config, registration, login throttle, password reset, email verification, Google OIDC, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 threads, PRO 5 mailboxes, BUSINESS unbounded), banners, trial-end |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, drip emails, operator metrics (`/admin/**`, allowlist `admin.emails`) |

## Data model (Flyway tables)
`users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts` (encrypted IMAP/SMTP
creds or Gmail OAuth refresh token, polling policy, last-seen UIDs, signature), `participants`,
`email_threads` (owner, `conversation_key`, unread), `messages` (incl. outbound flag), `message_recipients`,
`attachments` (`blob_ref` always null — bytes are not stored), `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `teams`,
`team_members`, `team_invites`. A *conversation* is a participant-set (`conversationKey`) spanning many
threads; the chat view merges them into one timeline of sender runs.

## Auth model
Session cookie (`JSESSIONID`) + CSRF on all POSTs (exempt: `/billing/webhook`, `/h2-console/**`),
remember-me 30 days, Google OIDC when `GOOGLE_CLIENT_ID` set. Public paths are whitelisted in
`SecurityConfig`; everything else requires login. **There is no token/bearer auth and no CORS config** —
a mobile client cannot authenticate yet (see `INTERNAL_TODO.md`).

## Web ↔ REST relationship
Today there is **no REST layer**. All controllers return Thymeleaf views or redirects, except
`POST /chats/refresh` (JSON `{imported:n}`), `GET /chats/events` (SSE) and Stripe webhook. Business logic
lives in services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`…), but `ConversationController` still orchestrates send/compose logic (recipient
parsing, sender selection, signature append, reply-vs-new) inline — it must be extracted to a service
before a JSON controller can share it. See `docs/API.md`.

## Commands
`./mvnw test`, `./mvnw clean package`, local run: see `CLAUDE.md`.

## Pre-existing planning docs
`master/` holds an older operator-routine set (`PLAN.md`, `BACKLOG.md`, `CHANGELOG.md` ~500 KB,
`MASTER_ACTIONS.md`) focused on billing/churn metrics. It is separate from the root `CHANGELOG.md`.
