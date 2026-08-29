# BACKEND.md — email-messenger architecture

Reverse-engineered from the code on `claude/relaxed-newton-utqmks` on
2026-08-29. Anything not observed in the source is marked **TBD**.

## Purpose

The backend, marketed as **ConexusMail**, is a Spring Boot 3 app on Java 21
that:

1. Serves the marketing site, product pages and legal pages via Thymeleaf.
2. Serves the authenticated web app (login/register, `/chats` workspace,
   account, mailboxes, billing, teams, digests, admin).
3. Imports mail from user-connected mailboxes (IMAP + Gmail OAuth) and
   transforms threads into a texting-style "conversation" view keyed by a
   participant-set `conversationKey`.
4. Bills users through Stripe (Free / Pro / Business tiers).
5. Sends transactional and digest email.

There is currently **no REST API layer for the mobile client** — every
Spring endpoint today serves a Thymeleaf template, a redirect, an SSE
stream, a bit of ad-hoc JSON (`POST /chats/refresh`), a PWA static asset,
a Stripe webhook, or SEO metadata. Building the mobile REST surface is
tracked in `docs/API.md` and `INTERNAL_TODO.md`.

## Module layout

`com.emailmessenger` is broken up by feature, not by layer:

- `EmailMessengerApplication.java` — Spring Boot entry point.
- `auth/` — form login, Google OIDC sign-in, registration, password reset,
  email verification, account settings, auth-event logging.
- `admin/` — admin metrics dashboard (revenue, funnel, churn, retention,
  weekly digest scheduler).
- `billing/` — Stripe checkout, customer-portal, cancellation reasons,
  plan-limit enforcement, webhook handler, billing period logic, trial-end
  and win-back email nudges.
- `web/` — Thymeleaf-facing controllers (marketing, `/chats`, mailboxes,
  saved searches, legal, SEO, PWA manifest/service worker, SSE for live
  updates).
- `service/` — cross-cutting services (email parsing, IM transform, thread
  building, reply sending, plan-limits).
- `domain/` — JPA entities.
- `repository/` — Spring Data JPA repositories.
- `email/` — IMAP integration (polling and IDLE), Gmail OAuth token
  handling.
- `team/` — team-workspace invites and member roles.
- `digest/` — weekly digest content assembly and opt-out.
- `activation/` — onboarding activation-nudge scheduler.
- `reengagement/` — inactive-user re-engagement scheduler.

## Persistence

- **PostgreSQL 16** in prod and local dev; **H2** for the test suite.
- **Flyway** migrations under `src/main/resources/db/migration`,
  `V1__init.sql` … `V35__mail_account_signature.sql`. Migrations are
  immutable once merged.
- **Spring Data JPA** (Hibernate) for reads and writes. The CLAUDE.md
  claims jOOQ in the routine's target stack, but the repo has never
  adopted jOOQ — everything is `spring-boot-starter-data-jpa`. **Note in
  API doc:** the "target stack" jOOQ line in the routine prompt does not
  match reality.

Local bootstrap is `db/init.sql` (creates database + app user). Config
reads `JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` /
`JDBC_DATABASE_PASSWORD` env vars.

## Auth model

Two sign-in paths, both producing a Spring Security session:

- **Form login** — email + password; passwords hashed by BCrypt. Password
  reset is token-based (`password_reset_tokens`).
- **Google OIDC** — sign-in through `/auth/google/start`, provisioned by
  `OAuth2ProvisioningService`. `users.google_subject` (V18) links the
  user to their Google identity; `users.password_set` (V19) distinguishes
  OIDC-only users from users who also set a password.

Email verification is enforced for form-login accounts via
`email_verifications` (V15) and the `/verify-email` endpoints. Auth events
are logged to `auth_events` (V16), consumed by the admin funnel metrics.

Login throttling is in-memory (`LoginThrottleService`), keyed by
username + IP.

The mobile client will **not** use session cookies. `docs/API.md` defines a
token-based scheme layered on top of the same `User` model.

## Chat model (the product's core)

The unit the UI (and mobile app) shows is the **conversation**, keyed by
a `conversationKey` (V32): a normalized, sorted set of the participants'
email addresses. A conversation aggregates every message the current user
has exchanged with that set of people across whatever email threads
happen to exist. Threads (`email_threads`) still exist for RFC 822
threading (Message-ID / In-Reply-To), but the `/chats` workspace and any
mobile screen keyed off it look at the conversation.

Key entities:

- `EmailThread` — a Message-ID chain. Owned by a user
  (`email_thread.owner_user_id`, V3), carries `conversation_key` (V32),
  `unread` (V9), and a `#thread` badge shown in the UI.
- `Message` — one imported email. Sender is a `Participant`. Body is
  cleaned at import time (`EmailBodyCleaner`) — quoted replies stripped,
  signature parsed off.
- `MessageOutbound` (V30) — an outbound send waiting to hit SMTP or
  already sent (used to reconcile Sent-folder imports for delivery
  status).
- `Participant` — deduped by normalized email; display name, avatar.
- `MailAccount` — a user's connected mailbox (IMAP or Gmail OAuth), with
  polling policy (V7), last-seen INBOX UID (V6), last-seen Sent UID (V31),
  auth type (V29), and signature (V35).
- `Subscription` — Stripe subscription + trial + billing period (V17,
  V23, V27, V28).
- `Plan` — Free / Pro / Business. Enforced by `PlanLimitService`.
- `Team` / `TeamMember` / `TeamInvite` (V24) — shared workspace.
- `SavedSearch` (V10, V11).
- `UserMailPreferences` (V34).

## Web (Thymeleaf) → Service → Domain

Controllers stay thin. All state mutation goes through the `service/`
layer or a feature-package service (`billing/`, `auth/`, `team/`, …).
The mobile REST controllers, once added, MUST call the same services so
web and mobile can never diverge on business rules.

## Live delivery

- **IMAP IDLE** for paid mailboxes (`ImapIdleService`, feature-flagged by
  `mailbox.idle.enabled`).
- **Polling** for everyone (`MailboxPollingService`), including Sent-folder
  sync (V31 cursor).
- **SSE** at `/chats/events` (`ChatEventStream`, `ChatEventsController`)
  pushes `NewMailEvent` to open web tabs so the chat rail and open pane
  morph in place without a page reload. The mobile client is expected to
  reuse this SSE stream (or a token-authenticated equivalent) for live
  updates; the exact shape is defined in `docs/API.md`.

## Scheduling

`@Scheduled` jobs (feature-flagged individually) power:

- Polling import.
- Weekly digest send.
- Weekly admin digest.
- Onboarding activation nudges (3 stages).
- Trial-end conversion nudges.
- Cancelled-subscription win-back nudges.
- Inactive-user re-engagement.

## Testing

- JUnit 5, Spring Boot Test.
- Testcontainers for Postgres, GreenMail for SMTP/IMAP integration tests.
- H2 in-memory DB for slice/unit tests via
  `src/test/resources/application.yml`.
- Run: `./mvnw test`.

## Build

- Maven wrapper (`./mvnw`). Full build: `./mvnw clean package`.
- Local run:
  `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
