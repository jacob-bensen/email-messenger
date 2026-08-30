# BACKEND — ConexusMail (email-messenger)

Spring Boot 3 / Java 21 / PostgreSQL 16 / Flyway / Maven. Serves both the
public website (Thymeleaf pages under `src/main/resources/templates/`) and
whatever machine-readable endpoints exist (see `API.md`). One codebase; the
web UI and any API paths must share the same service layer — never fork
business logic between them.

Note on scope: this document reverse-engineers the current tree. The project
was NOT originally designed with a separate mobile client in mind — it is a
SaaS ("ConexusMail") with a PWA shell. A general-purpose JSON API surface
does not yet exist. See `INTERNAL_TODO.md` for the resulting gaps.

## Module layout

Package root `com.emailmessenger`, split by concern:

```
com.emailmessenger
├── EmailMessengerApplication.java   entrypoint (Spring Boot)
├── admin/       operator dashboard (/admin/revenue, win-back queue)
├── activation/  new-user activation email drip
├── auth/        form + Google OIDC auth, SecurityConfig, password reset,
│                email verification, account settings
├── billing/     Stripe checkout + webhook + portal
├── digest/      weekly digest emails and opt-out
├── domain/      JPA entities (EmailThread, Message, Participant, ...)
├── email/       IMAP polling + Gmail OAuth + IMAP IDLE (live delivery pt.1)
├── reengagement/ win-back email drip for canceled subscribers
├── repository/  Spring Data JPA repositories
├── service/     email parsing (RFC 822 via Jakarta Mail), IM transform
│                (quoted-reply stripping, bubble grouping), reply sending
├── team/        team invites and membership
└── web/         Thymeleaf controllers, view models, SSE stream, PWA shell,
                 legal pages, marketing/pricing pages
```

Conventions (also in `CLAUDE.md`): constructor injection only, records for
DTOs and view models, package-private by default, Flyway migrations are
immutable once merged (add a new `V{n}__*.sql`, never edit an existing one).

## Domain model (V1__init.sql onward)

- `EmailThread` — groups messages by RFC 822 `Message-ID` / `In-Reply-To` /
  `References` chain; also carries a `conversation_key` (participant-set
  hash, V32) so IM-style chats stay merged even when the RFC threading
  breaks.
- `Message` — one email row: sender, recipients (JSON), plain + HTML body,
  sent-at, RFC message id, outbound/inbound flag (V30). Attachments are
  metadata-only today; `blob_ref` is always null (see roadmap).
- `Participant` — deduped by normalized email address, holds display name
  and avatar.
- `MailAccount` — a user's connected mailbox: IMAP host/port/creds
  (AES-encrypted at rest, keyed by `MAILBOX_ENCRYPTION_PASSWORD` +
  `MAILBOX_ENCRYPTION_SALT`) OR a Gmail OAuth refresh token (V29 adds
  `auth_type`). Carries `last_seen_uid` and `last_seen_sent_uid` cursors
  (V6/V31), polling policy (V7), signature (V35).
- `User` — auth identity: email, password hash (bcrypt), optional Google
  subject (V18), verification / activation / trial-end / re-engagement
  tracking flags scattered across V15–V28.
- `Subscription` — Stripe subscription mirror: plan (Free / Pro / Business
  or, in older copy, Personal / Team / Enterprise), status
  (active/canceled/past_due), current billing period (V17), cancellation
  reason (V27), win-back stamp (V28).
- `Team` + `TeamInvite` (V24) — shared workspaces for the Team plan.
- `SavedSearch` (V10/V11) — user-scoped stored queries visible on `/chats`.
- `PasswordResetToken` (V14), `EmailVerification` (V15), `AuthEvent` (V16)
  — auth plumbing.
- `PlanChangeEvent` (V26) — for churn/upgrade analytics on `/admin/revenue`.

Flyway migrations live under `src/main/resources/db/migration/V1..V35`.
Tests run against H2 (`src/test/resources/application.yml`); prod and local
dev use PostgreSQL 16. `db/init.sql` bootstraps the local Postgres role +
database.

## Auth model

- Spring Security session-cookie auth. Form login at `POST /login` (Spring
  Security-managed), OAuth2 login via Google at
  `/oauth2/authorization/google` (kicked off from `/auth/google/start`
  which captures plan/utm intent first).
- `SecurityConfig.java` marks marketing pages, auth pages, legal pages,
  `/billing/webhook`, `/billing/cancel`, `/digest/opt-out`, PWA assets, and
  the actuator health probe as `permitAll()`. Everything else requires an
  authenticated session.
- CSRF is on globally with two ignore paths: `/h2-console/**` (dev) and
  `/billing/webhook` (Stripe signs it separately via `Stripe-Signature`).
- Admin gate: `AdminAuthorizer.requireAdmin(...)` compares the current
  user's email against the comma-separated `ADMIN_EMAILS` env var (empty
  by default → nobody is admin).
- There is no bearer-token or API-key path today. Any mobile client would
  either need to reuse the session cookie (not designed for it — CSRF
  tokens on every form) or a new token-based auth surface would have to
  be added.

## Web (Thymeleaf) and REST/SSE surface

See `WEB.md` for the full page inventory and `API.md` for the
machine-readable endpoints (currently: Stripe webhook, `/chats/events` SSE
stream, `/chats/refresh` JSON heartbeat, PWA/SEO assets).

## Live delivery

- IMAP IDLE for paid mailboxes via `ImapIdleService`, guarded by the
  `mailbox.idle.enabled` flag. Publishes `NewMailEvent` when the server
  pushes a new message.
- `MailboxPollingService` also publishes `NewMailEvent` after each poll
  round.
- `ChatEventStream` fans events out over SSE to browser tabs subscribed
  to `GET /chats/events`; `chat.html` morphs the rail and open pane in
  place through `fragments/live-refresh.html`.
- Web Push (VAPID) through the PWA service worker is on the roadmap
  (roadmap: "Live delivery, part 2"); not implemented yet.

## Build and run

- Build: `./mvnw clean package`
- Run locally against local Postgres 16 (bootstrap with `db/init.sql`):
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `./mvnw test` (H2 in-memory, no external services required)

CI was intentionally removed on 2026-06-11 (see CLAUDE.md roadmap notes) —
tests run locally.
