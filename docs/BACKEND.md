# BACKEND

Architecture reference for the ConexusMail (email-messenger) backend.

## What this repo is

A Spring Boot 3 web app on Java 21. It owns:

- All authoritative business logic and persistence.
- The public website (Thymeleaf server-rendered pages).
- Email import (IMAP polling + IDLE) and reply send (SMTP).
- Auth (form + Google OIDC), billing (Stripe), teams, saved searches,
  digests, admin metrics, and a PWA shell.

It does **not** currently expose a JSON REST API for a mobile client. All
existing HTTP endpoints render HTML, accept form posts, or serve the Stripe
webhook and one SSE stream. See `docs/API.md` for the full picture and the
gap to close before `conexus-mail-mobile` can do anything real.

## Module map

Top-level packages under `com.emailmessenger` (see also `pom.xml`):

| Package        | Responsibility                                                       |
| -------------- | -------------------------------------------------------------------- |
| `web`          | Thymeleaf controllers, form objects, chat SSE stream.                |
| `service`      | Conversation building, IM transform, quoted-reply stripping, replies. |
| `email`        | IMAP client + IDLE, polling scheduler, MIME parsing, credential encryptor. |
| `domain`       | JPA entities (User, MailAccount, Message, EmailThread, ...).         |
| `repository`   | Spring Data JPA repositories.                                        |
| `auth`         | Registration, login (form + Google OIDC), password reset, email verification. |
| `billing`      | Stripe checkout/portal + webhook, plan state.                        |
| `team`         | Team + invite entities and controllers.                              |
| `activation`   | New-user activation drip + trial-end nudges.                         |
| `reengagement` | Digest emails and win-back outreach.                                 |
| `digest`       | Weekly/daily digest generation.                                      |
| `admin`        | Operator-only `/admin/revenue` metrics.                              |

## Domain overview

Core JPA entities and how they relate. Full definitions in `com.emailmessenger.domain`.

- `User` — end user account. Owns `MailAccount`s and `Subscription`.
- `Subscription` — Stripe subscription state per user. Plan, status, trial
  end, cancel-at, cancellation reason, win-back stamp.
- `MailAccount` — one connected mailbox. IMAP creds (encrypted) or Gmail
  OAuth refresh token; polling policy; last-seen UIDs; signature.
- `Participant` — deduped by normalized email address; display name.
- `EmailThread` — grouped messages, keyed by both RFC-822 threading and a
  participant-set `conversation_key`.
- `Message` — one email: subject, sender participant, plain + html body,
  sent-at, `Message-ID`, `In-Reply-To`, `References`, outbound flag, unread.
- `MessageRecipient` — many-to-many message ↔ participant, with `RecipientType`.
- `Attachment` — filename, mime, size, `blob_ref` (currently always null;
  attachment bytes are not stored yet — see `INTERNAL_TODO.md`).
- `SavedSearch`, `Team`, `TeamInvite`, `TeamMember`, `AuthEvent`,
  `EmailVerificationToken`, `PasswordResetToken`, `PlanChangeEvent`,
  `DigestEmailPreference`, `CancellationReason` — see the `domain/` package.

## Database

- PostgreSQL 16 in dev and prod. H2 for tests
  (`src/test/resources/application.yml`).
- Schema evolves via Flyway. Migrations live in
  `src/main/resources/db/migration/V{n}__*.sql` and are immutable once merged.
  Latest migration at this writing is `V35__mail_account_signature.sql`.
- ORM: **Spring Data JPA** (Hibernate). This repo does **not** use jOOQ.

## Auth

- Form login + registration under `AuthController` / `AccountController`;
  password hashing via Spring Security.
- Google OIDC via `spring-boot-starter-oauth2-client`, provisioning through
  `OAuth2ProvisioningService`. Gmail scope enables OAuth mailbox connect.
- Login throttling in `LoginThrottleFilter` + `LoginThrottleService`.
- Audit trail via `AuthEvent` / `AuthEventService`.
- Session-based (cookie). No JWT / API token flow today — see
  `INTERNAL_TODO.md` for the mobile-client blocker.

## Email pipeline

`MailboxPollingService` runs on a schedule (`@Scheduled`,
`MailboxPollingScheduler`), pulling INBOX and Sent-folder UIDs since the
last cursor per mailbox. For paid accounts, `ImapIdleService` maintains
IMAP IDLE connections (`mailbox.idle.enabled` feature flag). Both push new
messages through `EmailImportService` → `MimeMessageParser` →
`IMTransformService` (`EmailBodyCleaner` strips quoted replies and
signature blocks). New messages are threaded by both RFC-822
`Message-ID`/`In-Reply-To`/`References` and by a participant-set
`conversation_key` (V32).

Outbound sends go through `OutboundMessageService` / `ReplyService` via
`JavaMailSender`, then get re-imported from the sender's Sent folder (V31
cursor).

## Live delivery

- `NewMailEvent` published by `MailboxPollingService` and `ImapIdleService`.
- `ChatEventStream` fans out an SSE stream at `/chats/events` (per-user,
  filtered).
- `chat.html` + `fragments/live-refresh.html` morph the open pane in place.

Web Push (VAPID) for closed-tab notifications is on the roadmap; see
`CLAUDE.md`.

## Billing

- Stripe Checkout for upgrade (`/billing/checkout`), Customer Portal for
  changes (`/billing/portal`), and cancel (`/billing/cancel-subscription`).
- Webhook at `POST /billing/webhook` — the only JSON REST endpoint in the
  app today.
- Plans: Free / Personal / Team / Enterprise; annual variants.
- Feature gating via `PlanLimitKind`.

## Tests

- JUnit 5 + Spring Boot Test.
- Testcontainers for Postgres integration tests; GreenMail for IMAP/SMTP.
- Run everything: `./mvnw test`. Single test: `./mvnw test -Dtest=ClassName#method`.

## Local run

See `CLAUDE.md`. Bootstrap Postgres with `db/init.sql`, then
`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.

## Related repositories

- `jacob-bensen/conexus-mail-mobile` — Flutter mobile client. Currently
  scaffold-only; blocks on this backend exposing a REST API. See
  `../conexus-mail-mobile/docs/DESIGN.md` and this repo's `docs/API.md`.
