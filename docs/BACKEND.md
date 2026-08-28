# Backend architecture — email-messenger (ConexusMail)

Spring Boot 3 web app on Java 21. Serves both the marketing/product
website (Thymeleaf) and — in the near future — a REST API for the
mobile client at `conexus-mail-mobile`. Persists to PostgreSQL 16 in
prod/local dev, H2 for tests. Flyway owns the schema.

## Module map

```
com.emailmessenger
├── EmailMessengerApplication.java   # Spring Boot entry point
├── activation/                      # New-user onboarding nudges
├── admin/                           # /admin/* operator screens
├── auth/                            # form-login + Google OIDC, SecurityConfig
├── billing/                         # Stripe checkout/portal/webhook, plan gates
├── digest/                          # scheduled digest emails
├── domain/                          # JPA entities (no logic)
├── email/                           # IMAP/SMTP integration, polling, IDLE
├── reengagement/                    # win-back flows for at-risk subs
├── repository/                      # Spring Data JPA repositories
├── service/                         # transforms: threading, conversation view,
│                                    # reply-send, chat conversation model
├── team/                            # team invites / membership
└── web/                             # Thymeleaf controllers, form/DTO records
```

Controllers today all return HTML for Thymeleaf. There is one non-HTML
web endpoint of note: `GET /chats/events` streams Server-Sent Events
for live in-place chat updates (`ChatEventStream`, published by
`MailboxPollingService` via `NewMailEvent`).

## Auth model

`auth/SecurityConfig` configures a single Spring Security filter chain:

- **Form login** at `POST /login` — email + password. `LoginThrottleFilter`
  runs before it to lock out brute-force attempts.
- **Google OIDC** at `/auth/google/start` — wired only when
  `GOOGLE_CLIENT_ID` is set. `GoogleOidcUserService` maps the OIDC user
  to the local `User` row.
- **Session cookie** (`JSESSIONID`) + optional **remember-me** cookie
  backed by `JdbcTokenRepositoryImpl` (30-day validity).
- **CSRF** on for all state-changing endpoints. Exempted:
  `/h2-console/**`, `/billing/webhook` (Stripe signs the raw body).
- **Anonymous-allowed paths**: landing (`/`), pricing, demo, auth pages,
  password-reset, email verify, legal pages, digest opt-out,
  robots/sitemap, PWA shell (`manifest.webmanifest`, `sw.js`, `offline`,
  icons), static assets, actuator health, H2 console.

Everything else requires an authenticated session. There is currently
**no mobile-facing token/OAuth path**. That is the next thing to build
so the mobile client has something to authenticate against — see
`INTERNAL_TODO.md`.

## Persistence

- **Prod / local**: PostgreSQL 16 via `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars.
  `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`. Bootstrap with
  `psql -U postgres -f src/main/resources/db/init.sql`.
- **Tests**: H2 in-memory via `src/test/resources/application.yml`. All
  Flyway migrations must apply cleanly on both.
- **Migrations** are Flyway `V{n}__*.sql` in
  `src/main/resources/db/migration/`. Current tip is `V35` (mail account
  signature). **Never edit an applied migration — add a new one.**

## Core domain (JPA entities in `domain/`)

- `User` — the account holder. Auth via password hash or OAuth
  (`AuthType`).
- `Team`, `TeamMember`, `TeamInvite` — shared workspaces.
- `MailAccount` — one connected mailbox per row. Holds IMAP/SMTP creds
  or an OAuth refresh token, polling policy, last-seen UID / last-seen
  sent UID, signature.
- `EmailThread` — the RFC-822 threading anchor, plus
  `conversationKey` (V32) — the participant-set key that groups threads
  into one "chat" in the texting-style UI.
- `Message` + `MessageRecipient` + `RecipientType` — one email.
- `Participant` — deduped by normalized address.
- `Attachment` — filename, mime, size. `blob_ref` is currently always
  null (attachment bytes are not stored yet; see backend roadmap in
  `CLAUDE.md`).
- `Subscription`, `Plan`, `PlanChangeEvent`, `CancellationReason` —
  billing state.
- `AuthEvent`, `AuthEventType` — audit trail.
- `EmailVerificationToken`, `PasswordResetToken` — one-time tokens.
- `SavedSearch` — user-saved filters on `/chats`.
- `DigestEmailPreference` — user-level opt-in for digests.

## Mail I/O

- **Import**: `email/` package. `MailboxPollingService` runs on a
  schedule and pulls new UIDs via Jakarta Mail IMAP; `ImapIdleService`
  keeps a long-lived IDLE connection open for paid-tier accounts
  (flagged by `mailbox.idle.enabled`).
- **Threading + IM transform**: `service/`. Threads are built from
  `Message-ID` / `In-Reply-To` / `References`. Quoted replies are
  stripped (`EmailBodyCleaner`), consecutive same-sender messages are
  grouped into bubble runs (`ChatConversation`), and per-participant
  conversation keys collapse multi-thread history into one chat.
- **Send**: `ReplyService` + `OutboundMessageService` — SMTP with the
  mailbox's own creds. Sent-folder sync (V31 cursor) later re-imports
  the message so it appears in the timeline.

## Website ↔ services

Every Thymeleaf controller goes through the same `service/` layer —
there is no logic in the templates and no duplicated business logic in
controllers. When the mobile REST API arrives it must reuse those same
services; controllers become thin adapters (HTML ↔ JSON).

## Run / test

- Build: `./mvnw clean package`
- Local run:
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Full tests: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`

Integration tests use Testcontainers (Postgres) and GreenMail
(SMTP/IMAP). No CI is configured; run `./mvnw test` locally before
committing.

## Related repositories

- **`jacob-bensen/conexus-mail-mobile`** — Flutter mobile client that
  will consume this backend's REST API. See `docs/API.md` (this repo)
  for the contract and the mobile repo's `docs/MOBILE.md` for its side.
