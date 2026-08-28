# BACKEND.md — email-messenger server architecture

The email-messenger backend is a **Spring Boot 3 web application** that
serves both the ConexusMail public website (Thymeleaf) and — going
forward — a REST API for the `conexus-mail-mobile` Flutter client. It
owns all persistence and business logic. The mobile client mirrors
domain rules but is never authoritative.

## Stack

- Java 21 (LTS)
- Spring Boot 3.5.x
- Spring Data JPA + Hibernate (NOT jOOQ — the routine's default assumption
  does not apply to this repo)
- PostgreSQL 16 (prod / local dev), H2 for tests
- Flyway migrations under `src/main/resources/db/migration/`
- Thymeleaf server-side rendering for the website
- Spring Security (form login + Google OIDC)
- Jakarta Mail for IMAP / SMTP
- Stripe SDK (billing)
- Maven (with wrapper); JUnit 5 + Spring Boot Test + Testcontainers + GreenMail

## Top-level package layout — `com.emailmessenger`

| Package        | Purpose |
| -------------- | ------- |
| `activation`   | Post-signup activation nudges + scheduler |
| `admin`        | Operator dashboard: revenue / churn / funnel / retention metrics + backfill jobs |
| `auth`         | Registration, login, Google OIDC provisioning, password reset, email verification, session throttling, `SecurityConfig` |
| `billing`      | Stripe checkout / portal / webhook plumbing, plan limits, trial-end + win-back schedulers, billing banners, `PlanLimitService` |
| `digest`       | Weekly-digest scheduler, opt-out endpoint |
| `domain`       | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Plan`, `Team`, `TeamMember`, `SavedSearch`, `AuthEvent`, tokens, …) |
| `email`        | Email-import cursor helpers (`ConversationKeyBackfill`) — the actual IMAP polling + IDLE lives under `service/mailbox/` |
| `reengagement` | Dormant-user tracking + reengagement mail |
| `repository`   | Spring Data JPA repositories |
| `service`      | View models, threading + IM transform, conversation-key derivation, reply pipeline, body cleaning |
| `team`         | Team invite endpoints |
| `web`          | Thymeleaf controllers, view models, form objects, SSE stream, PWA, SEO |

`service.mailbox.*` — TBD (not yet inventoried in this doc; grep
`MailboxPollingService`, `ImapIdleService`, `NewMailEvent`).

## Domain model (essentials)

- **User** — account holder; owns `MailAccount`s, `Subscription`s,
  `Team`, `SavedSearch`es, preferences.
- **MailAccount** — one connected mailbox (IMAP password or Gmail OAuth);
  stores last-seen UID, last-seen sent UID, auth type, signature.
- **EmailThread** — grouped by RFC 5322 headers (`Message-ID` /
  `In-Reply-To` / `References`); owns unread flag, `conversationKey`
  (participant-set) used by the /chats view.
- **Message** — one email; text + HTML body, sent-at, outbound flag,
  attachments, recipients.
- **Participant** — deduped by normalized email; display name + avatar.
- **Attachment** — filename, mime, size, `blob_ref` (currently always
  null — attachments aren't yet downloadable; see roadmap).
- **Subscription** — Stripe subscription mirror: plan, status, current
  period end, trial end, cancellation reason.
- **Plan** — `FREE`, `PRO`, `BUSINESS`.
- **Team** / **TeamMember** / **TeamInvite** — team plan seats.
- **SavedSearch** — saved query + last-viewed-at.

## Auth model (current — website only)

- **Form login** at `POST /login`, guarded by `LoginThrottleFilter` +
  `LoginFailureHandler`.
- **Google OIDC** (`/oauth2/authorization/google`, initiated from
  `/auth/google/start`). Consent screen is still in **Testing** — Gmail
  refresh tokens expire after 7 days per Google policy; users must
  Reconnect weekly until the app is published.
- **Session cookie** (`JSESSIONID`), CSRF enabled on POST forms.
- **Admin access** — allowlist under `application.yml` `admin.emails`;
  non-admins get 404 on `/admin/**`.

The mobile client will need a **token-based API auth surface** — this does
not yet exist. See `docs/API.md` §Auth for the planned model.

## How Thymeleaf pages relate to the (future) REST API

Every state-mutating operation MUST go through the same service layer for
both the web pages and the REST API. Controllers are transport adapters
only. Concrete rules:

- Reply send: `web.OutboundMessageService` → `service.ReplyService` →
  domain writes. Any mobile `POST /api/chats/{key}/messages` must call the
  same `ReplyService.send(...)` path.
- Conversation view: `web.ConversationController` → `service.ConversationService`
  + `service.IMTransformService` → view model. A mobile
  `GET /api/chats/{key}` returns the same underlying data, serialized as
  JSON via a DTO layer (does not yet exist).
- Mailbox connect / sync: `web.MailboxController` +
  `web.GmailConnectController` → mailbox services. A mobile
  `POST /api/mailboxes` must call the identical service.
- Billing: `billing.BillingController` → `billing.BillingService` /
  `StripeCheckoutGatewayImpl`. Mobile subscription flows go through
  Stripe's mobile-native SDKs; the server-side gateway is shared.

**Never** duplicate business logic in a controller (web or REST) or in a
Thymeleaf fragment.

## Persistence + migrations

- Flyway runs on startup. Migrations are **immutable once merged** — add
  `V{n+1}__*.sql`, never edit an old one.
- Current highest migration: `V35__mail_account_signature.sql`.
- Local bootstrap: `psql -U postgres -f src/main/resources/db/init.sql`
  creates the `email_messenger` database and `email_messenger_app` role.

## Scheduled jobs

- `MailboxPollingService` — periodic IMAP fetch (feature-flagged via
  `MAILBOX_POLLING_ENABLED`).
- `ImapIdleService` — long-lived IMAP IDLE for paid-tier accounts.
- `ActivationScheduler`, `WeeklyDigestScheduler`,
  `TrialEndConversionScheduler`, `AdminWeeklyDigestScheduler`,
  `WinBackOutreachService` — periodic email flows.

## Configuration

See root `README.md` §Configuration for the environment-variable table.
Reproduced summary:

- **Required**: `JDBC_DATABASE_URL` / `_USERNAME` / `_PASSWORD`,
  `MAIL_HOST` / `_PORT` / `_USER` / `_PASS`,
  `MAILBOX_ENCRYPTION_PASSWORD` / `_SALT`.
- **Optional**: `GOOGLE_CLIENT_ID` / `_SECRET` (enables Google sign-in),
  `STRIPE_SECRET_KEY` / `_WEBHOOK_SECRET` (enables billing),
  `MAILBOX_POLLING_ENABLED` (default `true`).

## Related repositories

- **`conexus-mail-mobile`** — Flutter mobile client. Consumes the JSON API
  documented in `docs/API.md`. Mirrors domain rules under `lib/domain/`.
