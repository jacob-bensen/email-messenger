# Backend architecture

Verified against the code on 2026-10-31. Anything not implemented is marked **TBD**.

## Stack (actual)

Java 21, Spring Boot 3, Spring Security, **Spring Data JPA / Hibernate** (not jOOQ —
there is no jOOQ dependency and no codegen step), PostgreSQL 16 (H2 in tests),
Flyway (35 migrations, `V1`–`V35`), Thymeleaf, Maven wrapper, Stripe, Jakarta Mail
(IMAP/SMTP), Google OAuth (OIDC sign-in + Gmail mailbox OAuth).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = `/chats`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `PwaController`, `SeoController`, `ChatEventsController` SSE), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), view-model records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…) |
| `email` | IMAP import/poll/IDLE (`MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`), credential encryption, Gmail OAuth client |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (Free 3 mailboxes/500 threads; Pro 5/unlimited; Business unlimited), trial/win-back schedulers |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, weekly digest, lifecycle emails, admin revenue metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Data model (key tables)

`users`, `subscriptions`, `mail_accounts` (per-user IMAP/Gmail-OAuth account; password
ciphertext; poll cursors `last_seen_uid`, `last_seen_sent_uid`), `email_threads`
(has `conversation_key` = participant-set key, V32), `messages` (incl. outbound flag, V30),
`participants`, `message_recipients`, `attachments` (`blob_ref` always null — bytes are
not stored), `saved_searches`, `teams`/`team_members`/`team_invites`,
`digest_email_preferences`, token tables, `auth_events`, `plan_change_events`,
`persistent_logins` (remember-me).

## Auth model

- **Browser session only.** Form login (`POST /login`, params `email`, `password`,
  `remember-me`) or Google OIDC (`/auth/google/start`). `JSESSIONID` cookie; 30-day
  persistent remember-me token.
- **CSRF is ON** for every state-changing request except `/billing/webhook` and
  `/h2-console/**`.
- Everything not in the permit-list in `auth/SecurityConfig` requires authentication;
  unauthenticated requests redirect to `/login` (HTML), not 401 JSON.
- **No token/bearer auth exists. TBD** (required before a mobile client can talk to the
  server — see `INTERNAL_TODO.md`).

## Web ↔ API relationship

There is **no REST API**. The only machine-readable endpoints are
`POST /chats/refresh` (JSON `{"imported":n}`), `GET /chats/events` (SSE) and the Stripe
webhook. All user-facing data is rendered server-side by Thymeleaf from the same
services a future API would reuse (`ConversationListService`, `ChatService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`).

Known logic that lives in controllers rather than services (would need extraction before
an API can share it): recipient parsing, sending-account selection, signature
appending, and the send→record orchestration in `ConversationController`
(`createMessage`/`send`), mailbox scope resolution (`selectMailbox`, `scopeAddress`).

## Commands

See `CLAUDE.md`. Tests: `./mvnw test` (or `mvn -B test`).
