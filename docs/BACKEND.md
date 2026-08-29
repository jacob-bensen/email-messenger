# BACKEND

Snapshot of the backend as it exists on this branch. Companion to
`docs/API.md` (contract with any client) and `docs/WEB.md` (server-rendered
website).

## Runtime

- Java 21, Spring Boot 3.x, Maven (wrapper).
- Persistence: Spring Data JPA + Hibernate (`ddl-auto: validate`). Schema is
  owned by Flyway migrations under `src/main/resources/db/migration/`.
- Database: PostgreSQL 16 in dev/prod (env vars `JDBC_DATABASE_URL`,
  `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`); H2 in tests via
  `src/test/resources/application.yml`.
- View layer: Thymeleaf under `src/main/resources/templates/`. There is no
  general-purpose REST/JSON API yet (see `docs/API.md`).
- Mail: Jakarta Mail for IMAP import + SMTP send; Spring `JavaMailSender`
  for transactional email.
- Auth: Spring Security form login + Google OIDC (activated only when
  `AUTH_GOOGLE_CLIENT_ID` / `AUTH_GOOGLE_CLIENT_SECRET` are set).
- Billing: Stripe (Checkout + Customer Portal + one webhook receiver).
- Live delivery: SSE stream at `/chats/events`; IMAP IDLE for paid tiers
  (behind `mailbox.idle.enabled`).

## Package layout

Root: `com.emailmessenger`.

- `admin/` — operator dashboard, revenue/retention services, weekly digest,
  win-back outreach.
- `activation/` — activation and trial-end drip scheduling.
- `auth/` — form auth, OAuth start, account settings, email verification,
  password reset.
- `billing/` — Stripe checkout, portal, cancellation, webhook.
- `digest/` — subscriber digest opt-out endpoint.
- `domain/` — JPA entities (see below).
- `email/` — IMAP integration, IDLE service, polling job, body cleaner.
- `reengagement/` — win-back nudges.
- `repository/` — Spring Data JPA repositories.
- `service/` — email parsing, IM-style conversation transform, threading
  key derivation.
- `team/` — teams and invites.
- `web/` — Thymeleaf-facing controllers for mailboxes, chats, saved
  searches, PWA assets, SEO pages, legal, pricing, demo, and the SSE
  event stream.

## Domain entities

Under `com.emailmessenger.domain`:

`User`, `AuthEvent`, `AuthEventType`, `AuthType`, `EmailVerificationToken`,
`PasswordResetToken`, `Subscription`, `Plan`, `PlanChangeEvent`,
`CancellationReason`, `DigestEmailPreference`, `MailAccount`,
`Participant`, `EmailThread`, `Message`, `MessageRecipient`,
`RecipientType`, `Attachment`, `SavedSearch`, `Team`, `TeamMember`,
`TeamMemberRole`, `TeamInvite`.

## Flyway migrations

Immutable once merged. Current head is at `V35__mail_account_signature.sql`;
add new `V{n}__*.sql` files rather than editing existing ones.

## Configuration knobs

Sourced from `application.yml` / `application-local.yml` and environment
variables. Non-exhaustive:

- Database: `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`,
  `JDBC_DATABASE_PASSWORD`.
- Mail SMTP: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASS`.
- Mailbox encryption: `MAILBOX_ENCRYPTION_PASSWORD`,
  `MAILBOX_ENCRYPTION_SALT`.
- Google OAuth: `AUTH_GOOGLE_CLIENT_ID`, `AUTH_GOOGLE_CLIENT_SECRET`.
- Stripe: `STRIPE_PUBLIC_KEY`, `STRIPE_SECRET_KEY`,
  `STRIPE_WEBHOOK_SECRET`, `STRIPE_*_PRICE_ID` (six).
- Admin / digests / drips: `ADMIN_EMAILS`, `ADMIN_WEEKLY_DIGEST_ENABLED`,
  `ACTIVATION_ENABLED`, `TRIAL_END_ENABLED` (each with matching `_CRON` /
  `_ZONE` overrides).
- Live mail: `mailbox.idle.enabled`.

## Commands

- Build: `./mvnw clean package`
- Run locally: `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Tests: `./mvnw test` (H2 in-memory; no Docker required)
