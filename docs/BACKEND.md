# BACKEND — email-messenger (ConexusMail)

Spring Boot 3.x web application that imports email over IMAP/Gmail and renders
threads as instant-message conversations. It also serves the public website
(pricing, legal, marketing) and the operator admin surface.

## Stack

- Java 21, Spring Boot 3.5.x, Maven (wrapper: `./mvnw`)
- Persistence: **Spring Data JPA (Hibernate) + Flyway** on PostgreSQL 16
  (production/local) and H2 for tests. The generic full-stack routine calls
  for jOOQ; this project uses JPA, so treat that as the local convention.
- View: server-rendered **Thymeleaf** templates under
  `src/main/resources/templates`
- Security: `spring-boot-starter-security` (form login + BCrypt + CSRF) plus
  `spring-boot-starter-oauth2-client` for Google "Continue with Google" sign-in
- Mail: `spring-boot-starter-mail` (SMTP outbound) and Jakarta Mail for
  IMAP inbound (polling and IDLE)
- Billing: Stripe SDK (checkout + customer portal + webhook)
- PWA shell: manifest + service worker served from
  `com.emailmessenger.web.PwaController`

## Package layout (`com.emailmessenger`)

- `admin/` — operator dashboard (revenue, retention, win-back, weekly digest).
  `AdminRevenueController` mounts `/admin/revenue`.
- `activation/` — new-user activation nudges (day-2/day-4/last-chance).
- `auth/` — form auth, registration, password reset, email verification,
  Google OIDC callback; account settings (`/account/*`).
- `billing/` — Stripe checkout, portal, subscription state, cancel flow,
  webhook (`/billing/webhook`).
- `digest/` — daily/weekly digest emails and one-click opt-out endpoint.
- `domain/` — JPA entities (`EmailThread`, `Message`, `Participant`,
  `Attachment`, `MailAccount`, `User`, `Subscription`, `Team`, …).
- `email/` — IMAP integration: import, polling (`MailboxPollingService`),
  IDLE (`ImapIdleService`), sent-folder cursor, credentials.
- `reengagement/` — long-tail re-engagement email tracking.
- `repository/` — Spring Data JPA repositories.
- `service/` — parsing, IM transform (`IMTransformService`,
  `EmailBodyCleaner`, `BubbleRun`), conversation projections
  (`ChatConversation`, `ConversationKeyService`), reply send
  (`ReplyService`), demo data.
- `team/` — team creation, invites, membership.
- `web/` — Thymeleaf controllers: `/chats/**`, `/mailboxes/**`, `/pricing`,
  `/demo`, SEO/PWA assets, saved searches (`/searches`), SSE stream
  (`/chats/events`).

## Domain model (core entities)

- `Participant` (uq on normalized `email`) — one row per human address.
- `EmailThread` (uq on `root_message_id`) — RFC 822 chain plus derived
  `conversation_key` (participant set) for the IM view.
- `Message` (uq on `message_id_header`) — one email, plain + html bodies,
  `sent_at`, FKs to thread + sender.
- `MessageRecipient` — TO/CC/BCC join to participants.
- `Attachment` — filename, mime, size, `blob_ref` (null today; downloadable
  attachments are on the roadmap).
- `User`, `MailAccount` (IMAP or Gmail OAuth), `Subscription`, `Team`,
  `SavedSearch`, `AuthEvent`, plus activation/reengagement stamp tables.

Flyway migrations are immutable once merged. Current head: `V35__*.sql`.

## Auth model

- Form login (BCrypt) plus Google OIDC (`OAuthStartController`,
  `SecurityConfig` wires the login page).
- CSRF is enabled for state-changing form POSTs. Webhook and SSE endpoints
  are exempted where appropriate.
- Users may have `google_subject` set for OIDC identity; `password_set`
  distinguishes "OIDC-only" users so they can add a password later.

## Website (Thymeleaf) vs. API

Every state-changing HTTP handler today is a Thymeleaf/HTML controller that
POSTs a form and re-renders a template. **There is no JSON REST API yet.**
The one machine-readable endpoint is
`GET /chats/events` (SSE, `text/event-stream`) which pushes `newmail`
events to the open chat pane.

The mobile client will need a new `/api/v1/**` surface. Both the web pages
and the future JSON API must go through the same service classes
(`ConversationService`, `ReplyService`, `MailboxService`, `BillingService`,
`AuthService`) — controllers stay thin. See `docs/API.md` for the target
contract.

## Scheduled jobs

- `MailboxPollingService` — `@Scheduled` polling of connected mailboxes on
  Free-plan cadence; paid tiers also open `ImapIdleService` idle sessions.
- `AdminWeeklyDigestService` — Monday operator digest.
- Activation, re-engagement, and trial-end email schedulers under
  `activation/` and `reengagement/`.

## Configuration

- `application.yml` reads `JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` /
  `JDBC_DATABASE_PASSWORD` env vars for Postgres, plus Stripe, Google OAuth,
  SMTP, mailbox polling flags, and `mailbox.idle.enabled`.
- `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`. `db/init.sql` bootstraps the database
  and app user.
- Tests use H2 via `src/test/resources/application.yml`.

## Build / test / run

- Build: `./mvnw clean package`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`
- Run locally:
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
