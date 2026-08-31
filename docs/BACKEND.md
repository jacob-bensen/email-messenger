# BACKEND.md — email-messenger (ConexusMail)

This file is the concise architecture reference for the Spring Boot
backend. It complements the roadmap-heavy `CLAUDE.md` at the repo root
and the operator-facing `master/PLAN.md`. When it disagrees with the
code, the code wins; open a PR to fix this file.

## Role in the two-repo system

The backend is the authoritative half of a two-repo product:

- `email-messenger` (this repo) — Spring Boot 3 backend. Owns the
  database, all business logic, all email import, and BOTH the public
  website (Thymeleaf, `templates/`) and the JSON REST API that the
  mobile client consumes (`/api/v1/**`, see `docs/API.md`).
- `conexus-mail-mobile` — Flutter client. Talks to this backend over
  HTTPS + JSON per the contract in `docs/API.md`. Pure business rules
  in `lib/domain/` mirror server rules but never re-derive server state.

The website and the mobile app must go through the same service layer.
Duplicating rules in a controller or a template is a bug.

## Tech stack

- Java 21 (LTS).
- Spring Boot 3.5 — `starter-web`, `-thymeleaf`, `-data-jpa`,
  `-validation`, `-mail`, `-oauth2-client`, `-security`.
- PostgreSQL 16 in dev + prod; H2 for tests.
- Flyway for schema migrations (`src/main/resources/db/migration/`).
- Maven with wrapper (`./mvnw`).
- Testing: JUnit 5, Spring Boot Test, Testcontainers (Postgres),
  GreenMail (IMAP/SMTP).

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/     drip emails for newly-signed-up users
├── admin/          /admin/revenue + churn/retention/win-back services
├── auth/           form login, Google OIDC, password reset,
│                   email verification, security config
├── billing/        Stripe checkout, portal, webhooks, cancel flow
├── digest/         weekly saved-search digests + opt-out
├── domain/         JPA entities (User, Subscription, MailAccount,
│                   EmailThread, Message, Participant, Attachment,
│                   Team, SavedSearch, PlanChangeEvent, ...)
├── email/          IMAP polling + IDLE, SMTP send, thread stitching
├── reengagement/   nudges for dormant users
├── repository/     Spring Data JPA repositories
├── service/        top-level services (mailbox, conversation, chat,
│                   plan limits, mailbox encryption, ...)
├── team/           team-invite flow
└── web/            Thymeleaf controllers, form objects, chat SSE
                    stream, PWA manifest + service worker
```

## Persistence

- Every state mutation runs in a single transaction: read → validate →
  mutate → audit. Never trust client-supplied values for anything the
  server can compute (plan tier, MRR, entitlement, ownership).
- Schema is Flyway-managed. Migrations are immutable once merged — add
  `V{n}__*.sql` rather than editing an existing one.
- `MailAccount` credentials and OAuth refresh tokens are encrypted at
  rest with keys from `MAILBOX_ENCRYPTION_PASSWORD` +
  `MAILBOX_ENCRYPTION_SALT`; both env vars are required for a safe
  prod deploy (see `master/MASTER_ACTIONS.md`).

## Auth model

- Website: form login + Google OIDC via Spring Security. Session
  cookies. CSRF enforced on state-changing forms.
- Mobile REST API: TBD — no auth scheme is wired yet. Planned: bearer
  tokens minted from a `POST /api/v1/auth/login` endpoint scoped to
  mobile sessions, backed by the same `users` table. Do NOT enable
  session-cookie auth on `/api/**` (the mobile client does not manage
  cookies).
- Admin routes are gated by `AdminAuthorizer` on the comma-separated
  `ADMIN_EMAILS` env var. Empty means no operator can reach
  `/admin/revenue`.

## Email pipeline

- `MailboxPollingService` polls each active mailbox on a per-user
  cadence (`mail_account_polling_policy`), pulls new IMAP messages,
  and runs the sent-folder sync (`V31__mail_account_last_seen_sent_uid`).
- `ImapIdleService` (paid tier only, `mailbox.idle.enabled` flag)
  keeps a live IMAP IDLE socket per account for near-instant new-mail
  delivery.
- `ChatEventStream` publishes `NewMailEvent`s over an SSE stream
  (`GET /chats/events`) so the open web page morphs in place.
- Thread stitching keys off `Message-ID` / `In-Reply-To` /
  `References`. The `conversationKey` on `EmailThread`
  (`V32__email_thread_conversation_key`) groups messages by
  participant-set for the texting-style /chats workspace.

## Configuration

- Base `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars.
- `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`; `db/init.sql` bootstraps the
  database + app user.
- Prod profile is `prod`; deploy runs the packaged jar with
  `SPRING_PROFILES_ACTIVE=prod` — see `DEPLOY.md`.
- Docker + GitHub Actions CI were removed 2026-06-11 (owner
  decision); do not re-add.

## Build & test

- Compile + package: `./mvnw clean package`
- Run tests: `./mvnw test`
- Run locally: `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run
  -Dspring-boot.run.profiles=local`

## Conventions

- Constructor injection only; no field `@Autowired`.
- Records for DTOs + view models; entities are classes.
- Package-private by default; widen only when a cross-package caller
  needs it.
- Tests mirror the class-under-test's package under `src/test/java/...`.
- No backwards-compatibility shims while pre-1.0; change call sites.
- Don't comment what the code already says; comment non-obvious *why*.

## Related documents

- `CLAUDE.md` (root) — repo conventions + weekly roadmap.
- `master/PLAN.md` — current Primary Objective + milestones.
- `master/MASTER_ACTIONS.md` — human-owned tasks (creds, hosting, …).
- `master/CHANGELOG.md` — operator-facing daily log.
- `docs/WEB.md` — website page inventory.
- `docs/API.md` — REST API contract for the mobile client.
- `INTERNAL_TODO.md` — cross-repo sync backlog.
