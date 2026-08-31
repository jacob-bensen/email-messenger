# BACKEND.md — email-messenger (ConexusMail)

Architecture reference for the Spring Boot backend. This is the authoritative
server for the ConexusMail product: it owns the database, the business rules,
the Thymeleaf-served website, and (in future) the JSON API the mobile client
will consume. Reverse-engineered from source on 2026-08-31; keep in sync with
the code.

## Runtime

- Java 21, Spring Boot 3.x, Maven (wrapper).
- PostgreSQL 16 in production and local dev, H2 for tests.
- Flyway for schema migrations (`src/main/resources/db/migration/V{n}__*.sql`,
  immutable once merged).
- Persistence: **Spring Data JPA** (not jOOQ — the generic sync-routine
  document assumes jOOQ, but this codebase is JPA and entities live under
  `com.emailmessenger.domain`).
- Templating: Thymeleaf (`src/main/resources/templates/`).
- Scheduling: `@Scheduled` (mail polling, admin digests, activation,
  reengagement, trial-end conversion, weekly digests).
- Real-time: SSE (`/chats/events`) + IMAP IDLE (paid tier) behind
  `mailbox.idle.enabled`.

## Package layout (`com.emailmessenger.*`)

| Package        | Contents                                                                                 |
| -------------- | ---------------------------------------------------------------------------------------- |
| `activation`   | Post-signup activation nudges + scheduler.                                               |
| `admin`        | Admin dashboard: revenue, churn, funnel, at-risk retention, weekly digest.               |
| `auth`         | Form + Google OIDC login, account settings, password reset, email verification, throttling. |
| `billing`      | Stripe checkout, portal, webhook, plan limits, trial-end conversion, cancellation.       |
| `digest`       | Weekly digest emails + opt-out.                                                          |
| `domain`       | JPA `@Entity` classes and enums.                                                         |
| `email`        | IMAP polling + IDLE, Gmail OAuth, MIME parsing, credential encryption.                   |
| `reengagement` | Churn win-back scheduler.                                                                |
| `repository`   | Spring Data JPA repositories.                                                            |
| `service`      | View + domain services: `ConversationService`, `IMTransformService`, `ReplyService`,    |
|                | `EmailBodyCleaner`, view records for bubbles/chats.                                       |
| `team`         | Teams, invites, membership.                                                              |
| `web`          | Thymeleaf controllers, PWA controller, SSE stream, onboarding checklist, exception advice. |

## Domain model

See `com.emailmessenger.domain` for the authoritative classes. Summary:

- **`User`** — one row per account. Holds email, bcrypt password, `AuthType`
  (`PASSWORD` / `GOOGLE`), plan hints, appearance preferences, mail
  preferences (`sync_frequency_minutes`, `default_mail_account_id`), and
  activation/nudge tracking fields.
- **`MailAccount`** — a connected inbox owned by a user. Holds IMAP host/creds
  (encrypted via `CredentialEncryptor`) or Gmail OAuth refresh token,
  `PollingPolicy`, `last_seen_uid`, `last_seen_sent_uid`, `signature`.
- **`EmailThread`** — a grouping of `Message`s. Historically keyed by RFC 822
  `Message-ID`/`In-Reply-To`, now additionally keyed by `conversation_key`
  (V32) — a stable hash of the normalized participant set, used to render the
  chats workspace texting-style.
- **`Message`** — one email. Sender, subject, plain/html bodies, sent-at,
  `outbound` boolean (V30), attachments.
- **`MessageRecipient`** — one row per recipient (`RecipientType`: TO/CC/BCC).
- **`Participant`** — normalized email address + display name + avatar seed;
  deduped globally.
- **`Attachment`** — filename, mime, size, `blob_ref` (currently always null —
  attachments are not downloadable yet; roadmap item).
- **`Subscription`** — Stripe subscription mirror (`Plan`, `status`,
  `billing_period_start/end` per V17, `cancellation_reason` per V27,
  win-back tracking per V28).
- **`PlanChangeEvent`** — plan-change audit trail (V26).
- **`Team`, `TeamMember`, `TeamInvite`** — teams and pending invites (V24).
- **`SavedSearch`** — a user's saved chat filter (V10, V11 last-viewed).
- **`AuthEvent`** — auth-related audit event (V16).
- **`EmailVerificationToken`, `PasswordResetToken`** — one-shot tokens
  (V14, V15).

## Auth model

Defined in `com.emailmessenger.auth.SecurityConfig`.

- **Form login** at `POST /login` with `usernameParameter=email`, bcrypt
  password hashing (`BCryptPasswordEncoder`), users loaded through
  `DbUserDetailsService`.
- **Session cookies** (`JSESSIONID`) with a **remember-me** cookie backed by
  `JdbcTokenRepositoryImpl` (30-day, key `conexusmail-remember-me`).
- **Google OIDC** wired conditionally when a `ClientRegistrationRepository`
  bean is present (env-driven), using `GoogleOidcUserService` and
  `PlanCheckoutSuccessHandler` to redirect newly-provisioned users into the
  right onboarding step.
- **`LoginThrottleFilter`** runs before `UsernamePasswordAuthenticationFilter`
  and locks out repeated failures.
- **CSRF is enabled** for all non-webhook, non-H2-console routes. Any client
  that posts forms must fetch and echo the CSRF token via the standard
  Thymeleaf `<meta name="_csrf">` mechanism.
- **No API tokens, no JWT, no bearer auth, no `/api/**` matcher** exist
  today. A mobile client would need either (a) a new REST layer (planned;
  see `docs/API.md`) or (b) to drive the current HTML endpoints via the
  session cookie + CSRF token.

Public paths (allowed anonymously): `/`, `/pricing`, `/demo`, `/login`,
`/register`, `/auth/google/start`, `/password/*`, `/verify-email`, legal
pages, `/billing/cancel`, `/billing/webhook`, `/digest/opt-out`,
`/.well-known/**`, PWA assets (`/manifest.webmanifest`, `/icons/**`,
`/apple-touch-icon.png`, `/sw.js`, `/offline`), static `/css /js /images
/favicon.ico`, `/error`, `/h2-console/**`, `/actuator/health*`.

## Web vs API — shared service layer

Every Thymeleaf controller under `web/` (and every future REST controller)
must go through the same service layer (`service/`, `email/`, `billing/`,
`team/`, `auth/`, etc.). No controller or template may contain business
logic. Rationale: today the site is entirely server-rendered; when the
mobile client's REST layer lands, its endpoints will re-use the exact same
services so the app and site cannot drift.

## Migrations — high-level history

See `src/main/resources/db/migration/`. Highlights of the current state:

- **V32** `email_thread_conversation_key.sql` — adds
  `email_threads.conversation_key VARCHAR(64)` + index on
  `(owner_id, conversation_key)`. Backfilled by `ConversationKeyBackfill`.
- **V33** `drop_thread_notes.sql` — removes the internal notes-on-thread
  feature entirely.
- **V34** `user_mail_preferences.sql` — adds
  `users.sync_frequency_minutes INT` and
  `users.default_mail_account_id BIGINT FK -> mail_accounts`.
- **V35** `mail_account_signature.sql` — adds
  `mail_accounts.signature VARCHAR(2000)` used to auto-append a plain-text
  signature to outbound mail from that account.

New migrations MUST use `V36__*.sql` or higher and MUST NOT edit any file
already merged.

## Scheduled jobs

- `MailboxPollingScheduler` → `MailboxPollingService.pollAll()` on a fixed
  cadence per account (`PollingPolicy` + user-picked `sync_frequency_minutes`
  floored by plan limits).
- `ImapIdleService` — long-lived IDLE per paid-tier account, feature-flagged
  by `mailbox.idle.enabled`.
- `WeeklyDigestScheduler` — weekly digest.
- `ActivationScheduler` — post-signup activation nudges (day-1, day-3, etc).
- `ReengagementScheduler` — churn win-back outreach.
- `TrialEndConversionScheduler` — pre-trial-end conversion nudges.
- `AdminWeeklyDigestScheduler` — sends admin weekly rollup.
- Each scheduler is behind a feature flag so tests can disable them.

## SSE live delivery

`web/ChatEventStream` maintains per-user `SseEmitter`s. When
`MailboxPollingService` (or IDLE) imports new mail it publishes
`NewMailEvent`; a `@TransactionalEventListener(phase = AFTER_COMMIT)` in
`ChatEventStream` fans it out as an SSE `new-mail` event with payload
`{"imported": <int>}`. Keepalive: SSE comment every
`mailbox.sse.keepalive-ms` (default 25s). Emitter timeout: 30 min.

## PWA shell

- Manifest at `GET /manifest.webmanifest` (`web/PwaController`): name
  "ConexusMail", `start_url=/chats`, `display=standalone`,
  `theme_color=#2f855a`.
- Server-generated brand icons at `/icons/icon-192.png`,
  `/icons/icon-512.png`, `/icons/icon-512-maskable.png`,
  `/apple-touch-icon.png`.
- Service worker at `/sw.js` (versioned): pre-caches `/offline`,
  `/css/main.css`, `/icons/icon-192.png`, `/manifest.webmanifest`, and
  falls back navigations to `/offline` when offline.
- Registration fragment at `templates/fragments/sw-register.html`.

## Related repositories

- **`conexus-mail-mobile`** — Flutter mobile client. Mirrors the server's
  domain rules in `lib/domain/` (pure Dart). Consumes the API defined in
  this repo's `docs/API.md`.
