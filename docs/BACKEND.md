# BACKEND

Architecture reference for **email-messenger** (product name: ConexusMail /
MailIM), the Spring Boot server that both:

1. Serves the public **website + web app** via Thymeleaf server-rendered pages.
2. Owns **all persistence and business logic** (auth, mailbox connect, IMAP
   import, IM-style conversation rendering, teams, billing, digests, admin
   metrics).

At the time of writing, the backend does **not** expose a REST API for a mobile
client. Every controller returns HTML (or a redirect); the sole non-HTML
endpoint is the Stripe webhook. Building the REST surface for the sibling
`conexus-mail-mobile` client is a Phase 1 milestone tracked in `INTERNAL_TODO.md`.

## Stack

- Java 21 (LTS)
- Spring Boot 3.5 — `-web`, `-thymeleaf`, `-data-jpa`, `-validation`,
  `-security`, `-mail`, `-actuator`, `-oauth2-client`
- PostgreSQL 16 (production and local dev); H2 for tests
- Flyway for schema migrations (`src/main/resources/db/migration/V{n}__*.sql`,
  currently through V28)
- Maven (with wrapper) for build; JUnit 5 + Spring Boot Test + Testcontainers
  + GreenMail for integration tests

**Note on the routine's assumptions:** the shared full-stack routine documents
its target stack as jOOQ. This project uses Spring Data JPA (Hibernate). Defer
to the code, not to the routine's default.

## Module map (`com.emailmessenger.*`)

| Package | Responsibility |
| --- | --- |
| `EmailMessengerApplication` | Spring Boot entry point. |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, account settings, login throttling, `SecurityConfig`. |
| `billing` | Stripe checkout, portal, cancellation, webhook (the only `@RestController`). |
| `domain` | JPA entities: `User`, `Mailbox`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team`, `SavedSearch`, `ThreadNote`, `PlanChangeEvent`, etc. |
| `repository` | Spring Data JPA repositories. |
| `email` | IMAP polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), SMTP send, Gmail OAuth token refresh, parsing (`EmailBodyCleaner`, threading by `Message-ID`/`In-Reply-To`/`References`). |
| `service` | Shared services that don't belong to a leaf package. |
| `web` | Thymeleaf controllers, view models, form objects, PWA endpoints, SSE stream (`ChatEventStream`, `ChatEventsController`), SEO/robots/sitemap. |
| `team` | Team invites and shared team state. |
| `digest` | Weekly user digest emails + opt-out link. |
| `admin` | `/admin/revenue` dashboard, at-risk retention queue, win-back outreach (EPIC-18). |
| `activation` | Activation drip and trial-end conversion emails (feature-flagged). |
| `reengagement` | Cold-user re-engagement tracking. |

## Data model highlights

- `EmailThread` groups messages by RFC 822 threading headers.
- `Message` — one email; sender, recipients, plain + HTML body, sent-at,
  attachments; `blob_ref` on attachments is currently always null (attachments
  are not yet downloadable — a roadmap item).
- `Participant` — deduped by normalized email address; display name + avatar.
- `Conversation` view model — messages sorted, quoted-reply text stripped,
  consecutive same-sender messages collapsed into bubble runs; keyed by
  participant-set `conversationKey`.
- `Subscription` — plan (Free / Personal / Team / Enterprise), status, Stripe
  IDs, billing period, cancellation reason, win-back email tracking.

## Auth model

- Cookie-session-based (Spring Security form login) plus Google OIDC.
- `remember-me` uses `JdbcTokenRepositoryImpl` (persistent tokens table).
- Login throttling via `LoginThrottleFilter` inside the Security chain.
- Email verification (`V15__email_verifications.sql`) is mandatory before
  sensitive actions.
- Password reset via signed tokens (`V14__password_reset_tokens.sql`).

There is **no bearer/JWT auth today.** Introducing one — most likely a
device-scoped opaque token stored in a new `mobile_sessions` table — is a
prerequisite for the mobile client. Tracked in `INTERNAL_TODO.md`.

## Web ↔ API relationship

Today, every user-facing behavior is implemented in a Thymeleaf controller
that delegates to a service. When REST endpoints get added for the mobile
client they **must** call those same services — never fork logic into a REST
controller. See `docs/WEB.md` for the page ↔ service map that will guide
this.

## Local dev & test

- `db/init.sql` bootstraps `email_messenger` DB + app user against a local
  PostgreSQL 16 instance.
- `application-local.yml` (`local` profile) points at `localhost:5432/email_messenger`.
- Build: `./mvnw clean package`
- Run (needs local Postgres): `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- All tests: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`

## Related repositories

- **`conexus-mail-mobile`** — Flutter mobile client (currently skeleton only).
  Depends on this backend for auth + all data. The shared contract lives in
  `docs/API.md` in this repo.

## Related docs (this repo)

- `docs/API.md` — REST contract (currently placeholder; endpoints TBD).
- `docs/WEB.md` — Thymeleaf page inventory.
- `master/PLAN.md` — the operator-facing product objective and milestones.
- `master/BACKLOG.md` — short-term backlog.
- `master/MASTER_ACTIONS.md` — work waiting on human action.
- `master/CHANGELOG.md` — full historical changelog (kept by the roadmap routine).
- `CHANGELOG.md` (top-level) — per-run notes from the full-stack sync routine.
- `INTERNAL_TODO.md` — sync-audit gaps, tech debt, deferred work.
