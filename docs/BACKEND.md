# BACKEND — email-messenger (ConexusMail)

Server-rendered Spring Boot SaaS. This document is a living map of the
backend; keep it current when structure changes.

## Role in the two-repo system

`email-messenger` is the BACKEND. It is authoritative for all persistence,
business rules, mailbox integration, and billing. It also serves the
public website via Thymeleaf. A separate frontend repo
(`conexus-mail-mobile`) will host the mobile client; the REST API that
client will consume does **not yet exist** — see `docs/API.md` for the
current (nearly empty) contract and the plan.

## Stack

- Java 21 (LTS), Spring Boot 3.5
- PostgreSQL 16 (H2 in tests), Flyway migrations
- Spring Data JPA (no jOOQ)
- Thymeleaf views + vanilla CSS
- Spring Security (form + Google OIDC)
- Jakarta Mail for IMAP / SMTP
- Stripe (Java SDK) for billing
- Maven (wrapper), JUnit 5, Spring Boot Test, Testcontainers, GreenMail

## Package layout (`com.emailmessenger`)

```
├── EmailMessengerApplication.java   Spring Boot main
├── admin/          Operator-only pages and cron (weekly digest, revenue)
├── activation/     Post-signup activation nudges
├── auth/           Sign-in, sign-up, password reset, email verification, OAuth start
├── billing/        Stripe checkout/portal/webhook, cancellation flow
├── digest/         User-facing digest opt-out + scheduled digest send
├── domain/         JPA entities
├── email/          IMAP polling / IDLE / SMTP send / body cleaner
├── reengagement/   Dormant-user cron
├── repository/     Spring Data repositories
├── service/        Business services (chat, threading, IM transform, plan limits, …)
├── team/           Team invites, membership
└── web/            Thymeleaf controllers (chats, mailboxes, PWA, SEO, legal, pricing, demo)
```

## Domain entities (JPA)

`User`, `Subscription`, `PlanChangeEvent`, `AuthEvent`,
`EmailVerificationToken`, `PasswordResetToken`, `MailAccount`,
`EmailThread`, `Message`, `MessageRecipient`, `Attachment`,
`Participant`, `SavedSearch`, `DigestEmailPreference`, `Team`,
`TeamMember`, `TeamInvite`.

Conversation grouping is keyed by `conversation_key` on `EmailThread`
(participant-set hash, added in V32). See migrations for the schema of
record.

## Persistence

- Base config in `application.yml` reads `JDBC_DATABASE_URL`,
  `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD` from env.
- `local` profile (`application-local.yml`) points at
  `localhost:5432/email_messenger`; bootstrap DB + role via
  `src/main/resources/db/init.sql`.
- Tests run against H2 via `src/test/resources/application.yml`.
- Schema is Flyway-owned. Migrations are immutable once merged — new
  changes go in `V{n}__*.sql`. Latest is V35 (as of this file).

## Auth

- Form login (BCrypt) or Google OIDC.
- Google is activated only when both `auth.google.client-id` and
  `auth.google.client-secret` are set — otherwise the OIDC config bean
  is not built and the "Continue with Google" button is hidden.
- `SecurityConfig` gates `/admin/**` behind an operator allowlist
  (`admin.emails`, comma-separated; hardcoded to `jbensen92@gmail.com`).
- Sessions are cookie-backed. There is **no** token-based auth for
  external clients today — the mobile client will need one (see
  `docs/API.md` / `INTERNAL_TODO.md`).

## Mailbox integration

- `MailAccount` holds per-user IMAP/SMTP credentials (encrypted with
  `CredentialEncryptor`, keyed by `mailbox.encryption.password` +
  `mailbox.encryption.salt`) or a Gmail OAuth refresh token.
- `MailboxPollingService` + `MailboxPollingScheduler` (`@Scheduled`)
  pull new mail on a cadence, gated by `mailbox.polling.enabled`.
- `ImapIdleService` holds an IMAP IDLE connection per paid-tier account
  for live delivery, gated by `mailbox.idle.enabled`.
- New mail publishes a `NewMailEvent` which reaches open browser tabs
  via the `/chats/events` SSE stream (`ChatEventsController` /
  `ChatEventStream`).
- Outbound sends go through `ReplyService` → SMTP; sent-folder sync
  reconciles the message back into the thread once it appears in Sent.

## Billing

- Stripe checkout + billing portal + webhook.
- `BillingWebhookController` verifies the signature against
  `billing.stripe.webhook-secret`.
- Plans: Free / Personal / Team / Enterprise (see
  `master/PLAN.md`); plan → limits mapping in `PlanLimitKind`.
- Both keys default to empty; when unset, billing is inert and the
  entire app runs in dev mode without Stripe.

## Scheduled jobs

All cron features are off by default and require an explicit
`*.enabled: true` to fire:

- Digest send (weekly per-user)
- Reengagement (dormant users)
- Activation nudges (post-signup)
- Trial-end reminders
- Admin weekly digest (operator inbox)

The mailbox polling + IDLE schedules are on by default, but each has a
kill-switch (`mailbox.polling.enabled` / `mailbox.idle.enabled`).

## Health / actuator

Only `/actuator/health` is exposed. The mail health indicator is
disabled explicitly so a flaky SMTP relay does not flap the whole app.

## Existing operator docs

`master/` (in the backend repo root) holds the operator/ops routine's
`PLAN.md`, `BACKLOG.md`, `MASTER_ACTIONS.md`, and `CHANGELOG.md`. Those
track live-launch and revenue work and are separate from the full-stack
sync docs under `docs/`.
