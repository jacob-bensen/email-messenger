# BACKEND.md — ConexusMail server

Spring Boot 3 + Java 21 monolith. Serves both the public **Thymeleaf
website** (marketing pages, auth flows, chats workspace, account, admin,
billing, digest opt-out, PWA shell) and — eventually — the **JSON REST API**
for the mobile client. See `docs/API.md` for the contract, `docs/WEB.md`
for the page inventory.

## Modules (`com.emailmessenger.*`)

| Package | Responsibility |
| --- | --- |
| `auth` | Form login, Google OIDC start/callback, registration, password reset, email verification, `/account/*` settings, `SecurityConfig`, login-throttle filter, audit listeners. |
| `billing` | Stripe checkout/portal/webhook, plan-limit enforcement (`PlanLimitService`), trial-end conversion nudge, cancellation flow. |
| `web` | Thymeleaf controllers for `/chats/**`, `/mailboxes/**`, `/searches/**`, pricing, PWA (`/manifest.webmanifest`, `/sw.js`, offline page, icons), SEO (`/robots.txt`, `/sitemap.xml`, OG image), legal pages, redirects. |
| `service` | Domain logic: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ChatService`, participant-set `ConversationKeyService`, saved-search evaluation. |
| `email` | IMAP polling (`MailboxPollingScheduler` / `MailboxPollingService`), IMAP IDLE for paid tiers (`ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), MIME parsing, credential encryption. |
| `domain` | JPA entities (see below). |
| `repository` | Spring Data JPA repositories, one per aggregate root. |
| `team` | Teams, seat management, invite accept flow. |
| `digest` | Weekly digest scheduler + opt-out. |
| `reengagement` | Dormant-user reengagement emails. |
| `activation` | New-user activation nudges. |
| `admin` | Operator-only revenue/retention dashboards, gated on `admin.emails` allowlist. |

## Persistence

- **Runtime:** PostgreSQL 16 (`JDBC_DATABASE_URL`/`_USERNAME`/`_PASSWORD` env vars).
  `application-local.yml` targets `localhost:5432/email_messenger`;
  bootstrap the DB and app user with `db/init.sql`.
- **Tests:** H2 via `src/test/resources/application.yml`.
- **Schema:** managed by Flyway (`src/main/resources/db/migration/V*.sql`,
  currently V1–V35). Migrations are immutable once merged.
- **ORM:** Spring Data JPA + Hibernate, `ddl-auto: validate`,
  `open-in-view: false`.

## Domain entities

`Attachment`, `AuthEvent`, `EmailThread` (with `conversationKey`),
`EmailVerificationToken`, `MailAccount` (IMAP creds encrypted at rest;
Gmail OAuth stores refresh token; `lastSeenUid`, `lastSeenSentUid`,
`pollingPolicy`, `signature`), `Message`, `MessageRecipient`,
`Participant` (deduped by normalized address), `PasswordResetToken`,
`PlanChangeEvent`, `SavedSearch`, `Subscription` (Stripe-linked),
`Team`, `TeamInvite`, `TeamMember`, `User` (with acquisition source
and appearance/mail preferences).

`EmailThread.conversationKey` is the sorted set of participant addresses.
Chats in `/chats` are keyed by it, not by RFC-822 threading, so replies
that spawn a new `Message-ID` chain stay in the same conversation.

## Auth model

- Form login via `SecurityConfig` + `DbUserDetailsService` (BCrypt).
- Google OIDC ("Continue with Google"): activated only when
  `auth.google.client-id`/`-secret` are non-blank. `SecurityConfig` keys
  off the `ClientRegistrationRepository` bean, which is only built by
  `GoogleOAuthClientRegistrationConfig` when both are present.
- Email verification required for form-registered accounts.
- Password reset via signed short-lived tokens.
- `LoginThrottleFilter` rate-limits failed logins.
- Admin allowlist (`admin.emails`) gates `/admin/**`; non-admins get 404.
- **No API tokens yet.** Every current endpoint uses browser session cookies.
  Mobile REST access will need a token/JWT scheme — see `docs/API.md` TBDs.

## Mailboxes & mail delivery

- **Connect:** `/mailboxes/new` supports IMAP-with-password and Gmail OAuth
  (`/mailboxes/gmail/connect`, `/callback`, `/reconnect/{id}`).
- **Import:** `MailboxPollingScheduler` runs `@Scheduled` (gated by
  `mailbox.polling.enabled`, on by default). `MailboxPollingService` pulls
  INBOX and Sent (`lastSeenUid`, `lastSeenSentUid` cursors), parses with
  `MimeMessageParser`, and hands off to `EmailImportService` for
  threading + participant-set keying.
- **Live delivery (paid tiers):** `ImapIdleService` holds an IMAP IDLE
  connection per account (`mailbox.idle.enabled`). New mail publishes a
  `NewMailEvent`; the browser gets it via SSE at `/chats/events`
  (`ChatEventStream` + `ChatEventsController`) and morphs the rail + open
  pane in place. Free tier stays on the polling cadence. Not yet verified
  against a real IDLE-supporting server end-to-end.
- **Send:** `ReplyService` composes and sends via
  `spring-boot-starter-mail`; the sent copy is re-imported on the next
  Sent-folder pass so it shows up as an outbound bubble.

## Billing

- Stripe checkout/portal via `BillingController` and the
  `StripeCheckoutGateway` / `StripePortalGateway` /
  `StripeSubscriptionGateway` / `StripeWebhookGateway` interfaces (impls
  suffixed `Impl`, kept behind interfaces for testability).
- Plans: Free / Pro / Business. `PlanLimitService` enforces caps
  (mailboxes, seats, attachment bytes) keyed by `PlanLimitKind`.
- `BillingWebhookController` (`/billing/webhook`) is the only
  `@RestController` in the codebase today.
- Trial-end nudge cron and win-back tracking live in `billing/`.

## Scheduled jobs (all off-by-default unless flagged)

- `MailboxPollingScheduler` — INBOX/Sent import.
- `ImapIdleService` — per-account IDLE loop.
- Digest, reengagement, activation, trial-end conversion, admin
  weekly-digest — each gated on its own `*.enabled` flag.

## Build & run

- **Build:** `./mvnw clean package`
- **Test:** `./mvnw test` (H2, no external services needed)
- **Local run:** bootstrap DB with `db/init.sql`, then
  `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

## Configuration surface (env vars)

`JDBC_DATABASE_URL` / `_USERNAME` / `_PASSWORD`, `MAIL_HOST`/`PORT`/`USER`/`PASS`,
`GOOGLE_CLIENT_ID`/`_SECRET`, `STRIPE_SECRET_KEY`/`_WEBHOOK_SECRET`,
`MAILBOX_ENCRYPTION_PASSWORD`/`_SALT`,
`MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`.
