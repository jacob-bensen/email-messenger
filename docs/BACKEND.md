# BACKEND.md

Architecture overview for the ConexusMail backend (`email-messenger`). This is a
Spring Boot 3 monolith that serves both the marketing/product website
(Thymeleaf) and the JSON REST API used by the ConexusMail mobile client.

## High-level

- **Java 21 + Spring Boot 3.5** monolith, packaged with Maven (wrapper checked
  in as `./mvnw`).
- **PostgreSQL 16** in production and local dev; **H2** for the test profile.
  Schema managed by **Flyway** (`src/main/resources/db/migration/V*.sql`).
- **Spring Data JPA** with entities in `com.emailmessenger.domain` and
  repositories in `com.emailmessenger.repository`. (We do NOT use jOOQ.)
- **Spring Security** for form + Google OIDC login, remember-me tokens
  (`persistent_logins`), and BCrypt password hashing.
- **Thymeleaf** for the marketing site and the authenticated chats web app.
- **Jakarta Mail + GreenMail** for IMAP polling / IDLE, SMTP send, and
  in-process tests.
- **Stripe** for billing (Checkout Sessions, subscriptions, webhooks).

## Module layout

```
com.emailmessenger
├── EmailMessengerApplication.java        Spring Boot entry point
├── activation/    account activation nudges + emails
├── admin/         admin console (metrics, revenue)
├── api/           JSON REST controllers used by the mobile client (this run)
├── auth/          Spring Security config, users, sessions, email verify, OAuth
├── billing/       Stripe integration (Checkout, subscriptions, webhooks)
├── digest/        transactional + digest email jobs
├── domain/        JPA entities
├── email/         IMAP polling / IDLE, SMTP send, message parsing
├── reengagement/  win-back emails for cancelled/lapsed users
├── repository/    Spring Data JPA repositories
├── service/       IM transform + conversation / bubble / reply services
├── team/          teams and invites
└── web/           Thymeleaf controllers, view services, and forms
```

Package-private is the default; visibility widens only where crossed-package
callers need it.

## Domain model highlights

- `User` — owns `MailAccount`s, `Subscription`, `Team` memberships.
- `MailAccount` — a connected mailbox (IMAP + password or Gmail OAuth). Tracks
  polling policy, IDLE readiness, per-folder `last_seen_uid` cursors, and (as of
  V35) the sender signature.
- `EmailThread` — RFC-822 threaded email chain, keyed additionally by a
  `conversation_key` (participant-set hash added in V32) so the chats view can
  merge unrelated threads with the same people into one conversation.
- `Message` + `MessageRecipient` — one email; direction (inbound/outbound) via
  the V30 migration.
- `Attachment` — filename/mime/size; `blob_ref` is currently always null (see
  the roadmap for planned blob store work).
- `Participant` — deduped by normalized email address; display name + avatar.
- `SavedSearch`, `Team`, `TeamInvite`, `TeamMember`, `Plan`, `Subscription`,
  `PlanChangeEvent`, `CancellationReason`, `DigestEmailPreference`,
  `AuthEvent`, `EmailVerificationToken`, `PasswordResetToken`.

## Service layer

All business rules live in `com.emailmessenger.service` (chat / IM transform),
`com.emailmessenger.email` (mailbox), `com.emailmessenger.auth` (accounts and
sessions), and `com.emailmessenger.billing` (Stripe). Both the Thymeleaf
controllers (`com.emailmessenger.web`) and the REST controllers
(`com.emailmessenger.api`) MUST call into the same service layer — never fork
business logic between web and API paths.

## Auth model

- **Website / PWA:** session-cookie based. `SecurityConfig` runs form login
  (`/login`, `email` + `password` parameters), Google OIDC (when the
  `GOOGLE_CLIENT_ID` env var is set), remember-me via JDBC tokens, and CSRF is
  ON for state-changing requests. `LoginThrottleFilter` runs before the
  form-login filter to lock out brute-force attempts.
- **Mobile client / REST API:** the endpoints under `/api/**` are intended to
  be stateless. As of this run only `GET /api/v1/health` exists and is
  `permitAll`; when the first authenticated mobile endpoint lands it will
  design the bearer-token flow (session cookies are not appropriate for the
  mobile client).

## Live delivery

- `MailboxPollingService` (scheduled, feature-flagged) fetches new mail and
  publishes `NewMailEvent`s.
- `ImapIdleService` holds IMAP IDLE connections open for paid-tier accounts.
- `ChatEventStream` bridges to a per-user SSE stream at `/chats/events` that
  the web app consumes via `fragments/live-refresh.html`.
- Web Push (VAPID) is on the roadmap.

## Runtime

- Base config: `src/main/resources/application.yml` reads
  `JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`.
- Local profile: `application-local.yml` points at
  `localhost:5432/email_messenger` (see `db/init.sql`).
- Test profile: `src/test/resources/application.yml` runs H2.
- Actuator: `/actuator/health` is `permitAll` and used by platform
  liveness/readiness probes. The mobile client instead hits
  `/api/v1/health`, which is our stable JSON contract (actuator's shape can
  change between Spring Boot versions).

## Commands

- Build: `./mvnw clean package`
- Run locally:
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`

## Related repositories

- **conexus-mail-mobile** — the Flutter mobile client. Depends on this
  repo's `docs/API.md` as the canonical wire contract.
