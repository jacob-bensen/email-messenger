# Backend architecture

Spring Boot 3 monolith. Serves both the Thymeleaf web app ("ConexusMail" at
conexusmail.com — planned) and, going forward, the REST API consumed by
`conexus-mail-mobile` (the Flutter client). One service layer, two front
doors.

## Modules

Top-level Java packages under `com.emailmessenger`:

- `auth/` — form login, Google OIDC, registration, email verification,
  password reset, session throttling, appearance/notification settings.
  `SecurityConfig` owns the single `SecurityFilterChain`.
- `web/` — Thymeleaf controllers (chats, mailboxes, saved searches,
  onboarding, SEO/PWA, legal), plus the SSE `/chats/events` stream.
- `service/` — business logic: conversation building, IM transform, reply
  composition, participant/conversation-key normalization. Called by both
  web controllers and (in future) REST controllers.
- `domain/` — JPA entities.
- `repository/` — Spring Data JPA repositories.
- `email/` — IMAP fetch (Jakarta Mail), Gmail OAuth, IMAP IDLE, mailbox
  polling scheduler, MIME parsing, `NewMailEvent` publisher.
- `billing/` — Stripe checkout, portal, subscription and webhook
  gateways, plan-limit enforcement, upgrade-modal helpers.
- `admin/` — internal metrics (revenue, churn, funnel, ProAdoption,
  trial-end conversion, win-back), controllers guarded by
  `AdminAuthorizer`.
- `activation/`, `reengagement/`, `digest/`, `team/` — scheduled
  outreach and multi-user features.

## Persistence

- PostgreSQL 16 in dev and production, H2 for tests. Base
  `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars;
  `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`; `db/init.sql` bootstraps the local
  DB + app user.
- Flyway migrations under `src/main/resources/db/migration/`. Latest
  applied is `V35__mail_account_signature.sql`; the next new migration
  must be `V36__*.sql`. Existing migrations are immutable — never edit
  one that has been merged.
- JPA/Hibernate on top of Flyway-managed schema. No jOOQ (the
  full-stack routine names jOOQ as the *target* but this project uses
  JPA — do not migrate away from JPA in a routine run).

## Auth model

- Server-authoritative sessions. Form login lives at `POST /login`
  (email + password) with a persistent `remember-me` token stored in
  the DB via `JdbcTokenRepositoryImpl`.
- Google "Continue with Google" via Spring Security OAuth2 client;
  provisioned on demand by `OAuth2ProvisioningService`. Wires in only
  when `GOOGLE_CLIENT_ID` is set.
- Password auth uses `BCryptPasswordEncoder`.
- `LoginThrottleFilter` runs before the username/password filter to
  short-circuit brute-force attempts.
- CSRF is on by default; `/h2-console/**` and `/billing/webhook` are
  the only exemptions today. Mobile REST endpoints under `/api/**`
  will be added as CSRF-exempt paths as they arrive (they authenticate
  via bearer token — TBD — not the session cookie).

## Web ↔ API relationship

The Thymeleaf pages and any REST endpoint MUST call the same service
layer under `service/`, `auth/`, `email/`, `billing/`. Controllers are
thin adapters. Do NOT duplicate business logic in a REST controller
that the web controller already implements — extract a shared service
first.

## Scheduled work

- `MailboxPollingScheduler` (`email/`) — periodic IMAP fetch, gated by
  plan.
- `ImapIdleService` (`email/`) — IMAP IDLE for paid tiers.
- `ActivationScheduler`, `ReengagementScheduler`,
  `AdminWeeklyDigestScheduler`, `WeeklyDigestScheduler`,
  `TrialEndConversionScheduler` — outbound email cadences.

## What is NOT yet built

- Mobile-facing REST API (only `/api/v1/health` exists today — added
  2026-08-29).
- Bearer/JWT auth for the mobile client (TBD; see
  `INTERNAL_TODO.md`).
- OpenAPI/Swagger generation — the API contract lives in `docs/API.md`
  and is maintained by hand.
