# Backend architecture

Reverse-engineered from the code on 2026-10-30. Unknowns are marked **TBD**.

## Stack (actual)

Java 21, Spring Boot 3.x, **Spring Data JPA / Hibernate** (`ddl-auto: validate`),
PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security, Maven.
There is **no jOOQ** in this project — see `INTERNAL_TODO.md`.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling, auth-event audit |
| `billing` | Stripe checkout/portal/webhook, plans & caps (`PlanLimits`), trial nudges, billing banner |
| `email` | Mailbox integration: IMAP client, IDLE, Gmail OAuth, polling, MIME parsing, import, credential encryption |
| `service` | IM transform: conversation keys, bubble runs, quote stripping, `ChatConversation` view models, `ReplyService` (SMTP send) |
| `web` | Thymeleaf controllers, chat workspace, mailboxes, saved searches, PWA, SEO, legal, SSE stream |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team` | Teams + invites |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails (off unless `*.enabled=true`) |
| `admin` | `/admin/**` metrics (operator allow-list, non-admin gets 404) |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)

Core: `users`, `subscriptions`, `mail_accounts` (encrypted IMAP/SMTP creds or
Gmail OAuth, per-account UID cursors for inbox and sent), `email_threads`
(owner, `conversation_key`, unread), `messages` (outbound flag),
`message_recipients`, `participants` (deduped by normalized address),
`attachments` (metadata only; `blob_ref` always null), `saved_searches`,
`teams`/`team_invites`/`team_members`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`,
`plan_change_events`, `persistent_logins` (remember-me).

A **conversation** is not a table: it is the set of threads sharing a
`conversation_key` (hash of the participant set minus the owner's own
addresses; `ConversationKeyService`). `ChatService` merges those threads into
one timeline.

## Auth model

Server-side **session cookie** (`JSESSIONID`) + optional 30-day remember-me
cookie. CSRF is **on** for every POST except `/billing/webhook` and
`/h2-console/**`. Login: `POST /login` (`email`, `password`). Google OIDC only
when `GOOGLE_CLIENT_ID` is set. Everything outside the public allow-list in
`SecurityConfig` requires authentication. **No token / API-key auth exists**,
so a mobile client cannot currently authenticate (see INTERNAL_TODO #1).

## Web vs REST

The app is server-rendered. The only non-HTML endpoints are
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook`
(Stripe), `/actuator/health`. There is **no `/api/**` namespace**. Controllers
delegate to services (`ChatService`, `ConversationListService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`, …), which is
the layer a future REST API must reuse. One known leak: `ConversationController`
holds send/compose orchestration (recipient parsing, signature append, reply vs
new-email branching) that should move into a service first (INTERNAL_TODO #3).

## Background work

`MailboxPollingScheduler` (poll), `ImapIdleService` (paid tiers, flag
`mailbox.idle.enabled`), `ChatEventStream` (SSE fan-out of `NewMailEvent`),
lifecycle schedulers (digest, reengagement, activation, trial-end, admin
weekly digest).
