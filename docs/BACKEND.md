# Backend architecture — email-messenger (ConexusMail)

The backend is a single Spring Boot 3 monolith that serves **both** the
public website / signed-in web app (server-rendered Thymeleaf pages) and,
in the near future, a REST API for the [conexus-mail-mobile] Flutter
client. All persistence, business rules, billing enforcement, and mail
I/O are owned here — the server is authoritative for every state
transition. The mobile client and any future web JS mirrors these rules
but never gets to decide state on its own.

[conexus-mail-mobile]: https://github.com/jacob-bensen/conexus-mail-mobile

## Runtime stack

- Java 21, Spring Boot 3.x
- Spring MVC + Thymeleaf for server-rendered pages
- Spring Data JPA on PostgreSQL 16 (H2 in tests)
- Flyway migrations under `src/main/resources/db/migration`
- Spring Security 6 (form login + Google OIDC)
- Jakarta Mail for IMAP / SMTP; Sun/Eclipse IMAP IDLE for live delivery
- Stripe Java SDK for billing
- Maven (with wrapper)

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   entrypoint
├── activation/     onboarding + activation-nudge scheduling
├── admin/          admin-only revenue / retention dashboard
├── auth/           login, registration, OAuth, password reset,
│                   email verification, SecurityConfig
├── billing/        Stripe checkout / portal / webhook, plan gating
├── digest/         weekly digest email + opt-out
├── domain/         JPA entities (see below)
├── email/          IMAP / SMTP integration, IDLE, imports
├── reengagement/   post-cancel win-back tracking
├── repository/     Spring Data repositories
├── service/        cross-cutting services (mail parsing, IM transform,
│                   plan limits, conversationKey grouping, SSE bus)
├── team/           team invites + members
└── web/            Thymeleaf controllers, form DTOs, SSE endpoint
```

## Core domain entities

`domain/`:

- `User` — the account. Owns mail accounts, subscriptions, saved
  searches, digest prefs, activation state, per-user mail preferences
  (sync cadence, default sending mailbox).
- `MailAccount` — a connected mailbox (IMAP+password or Gmail OAuth).
  Carries polling policy, `lastSeenUid` (INBOX), `lastSeenSentUid`
  (Sent folder — added V31), auth type, optional signature.
- `EmailThread` — a run of `Message`s linked by RFC 5322
  `Message-ID` / `In-Reply-To` / `References`. Owned by a `User`.
  Threads are further **grouped** into texting-style conversations by
  `conversationKey` — a hash of the normalized participant set (V32);
  the `/chats` UI keys off this, not the thread id.
- `Message` — one email: subject, plain + html body, sent-at,
  attachments, `outbound` flag (V30), Message-ID.
- `MessageRecipient` — recipient rows (to / cc / bcc) with normalized
  address + type enum.
- `Participant` — deduped by normalized address, holds display name and
  avatar seed.
- `Attachment` — filename, mime, size, `blobRef` (currently always
  null — attachment bytes aren't persisted yet).
- `Subscription`, `Plan`, `PlanChangeEvent`, `CancellationReason` —
  Stripe-backed billing state; Free / Pro / Business tiers.
- `SavedSearch` — named filters over `/chats`.
- `DigestEmailPreference` — per-user weekly-digest opt-in / opt-out
  token.
- `EmailVerificationToken`, `PasswordResetToken`.
- `AuthEvent`, `AuthEventType`, `AuthType` — audit + auth kind
  (`FORM`, `GOOGLE`).
- `Team`, `TeamInvite`, `TeamMember`, `TeamMemberRole`.

## Web layer vs. (future) REST API

Today every controller returns a Thymeleaf view name. The **only**
JSON-shaped endpoints are:

- `POST /billing/webhook` — Stripe webhook receiver.
- `POST /chats/refresh` — returns `{ "imported": <int> }` after a
  foreground poll.
- `GET /chats/events` — Server-Sent Events stream of `new-mail`
  notifications, keyed by user id.

Everything else is HTML-form + server redirect. See `docs/WEB.md` for the
page inventory and `docs/API.md` for the **planned** JSON API surface
the mobile client will consume. Both the website and the API must go
through the same service layer — never duplicate business logic in
controllers or templates.

## Live delivery (roadmap "Live delivery, part 1" — done)

`ImapIdleService` opens IMAP IDLE connections for paid-tier accounts
(gated on `mailbox.idle.enabled`). When new mail lands (or the polling
job imports something), `MailboxPollingService` publishes a
`NewMailEvent`; `ChatEventStream` fans it out through SSE to any open
`/chats` tab keyed by user id. The Thymeleaf pane is morphed in place
by `fragments/live-refresh.html`. This will need a mobile equivalent
(APNs / FCM push, plus a lightweight `/api/events` WebSocket or long-poll).

## Security

`auth/SecurityConfig` — form login (`POST /login`, username=email),
Google OIDC (`/oauth2/authorization/google`), remember-me, CSRF on
except webhook, permitAll matrix for public pages and static assets.
Every admin action calls `AdminAuthorizer.requireAdmin(principalName)`.
Plan-gated actions (extra mailboxes, faster polling, saved searches,
attachments) go through `PlanLimitEnforcer` and raise
`PlanLimitExceededException`, which `GlobalExceptionHandler` turns
into a redirect + upgrade flash.

## Billing

Stripe Checkout for new subscriptions, Stripe Customer Portal for
management, webhooks reconcile subscription state. `Plan` is Free / Pro
/ Business with monthly + annual pricing. `PlanChangeEvent` records
each transition; `Subscription.cancellationReason` (V27) captures why
someone left; `V28` and the win-back scheduler send a discount offer
some weeks after cancellation.

## Configuration

- `application.yml` — base config; reads `JDBC_DATABASE_URL`,
  `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD` env vars in prod.
- `application-local.yml` — the `local` Spring profile; points at
  `jdbc:postgresql://localhost:5432/email_messenger` as
  `email_messenger_app`.
- `src/test/resources/application.yml` — H2 in-memory for tests.
- `db/init.sql` — one-time bootstrap for the local Postgres cluster.

Feature flags of note (`application.yml` keys):

- `mailbox.idle.enabled` — enables IMAP IDLE live delivery.
- Stripe / Google OAuth / SMTP credentials are all `${ENV_VAR}` refs.

## Testing

- JUnit 5 + Spring Boot Test.
- Testcontainers for Postgres integration tests, GreenMail for
  fake IMAP / SMTP.
- H2 in-memory for the default unit-test profile.
- Migrations must apply cleanly on both Postgres and H2 (H2 in Postgres
  compatibility mode).

Run everything with `./mvnw test`; a single test with
`./mvnw test -Dtest=ClassName#method`.

## Related repositories

- **conexus-mail-mobile** — the Flutter mobile client. Uses this
  backend as its API server; mirrors business rules in
  `lib/domain/`. Sources of truth for cross-repo work:
  - `docs/API.md` (in this repo) — the API contract, canonical.
  - `docs/DESIGN.md` (in the mobile repo) — product design + roadmap.
