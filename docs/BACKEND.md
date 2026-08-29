# BACKEND — email-messenger

Server-authoritative Spring Boot application for the ConexusMail product.
It owns everything: the public marketing website, the signed-in web app
(`/chats`, `/mailboxes`, `/account`, `/admin/revenue`), all persistence,
IMAP/SMTP integration, Stripe billing, auth (form login + Google OIDC),
scheduled jobs, and the operator dashboards. Today this is a
single-deployable web application — there is **no separate REST API
surface** for a mobile client (see the "API surface" section below).

## Tech stack

- Java 21, Spring Boot 3.5.
- Thymeleaf server-rendered templates under `src/main/resources/templates`.
- Spring Data JPA over PostgreSQL 16 in prod/local, H2 in tests.
- Flyway migrations under `src/main/resources/db/migration/` — currently
  `V1__init.sql` through `V35__mail_account_signature.sql`. Never edit an
  applied migration; add a new `V{n}__*.sql`.
- Jakarta Mail for IMAP polling + IDLE and SMTP outbound.
- Spring Security: BCrypt form login, CSRF, session cookies; Google OIDC
  via `spring-boot-starter-oauth2-client` (client-side sign-in only, not
  bearer tokens).
- Stripe for billing (checkout, portal, webhook at `/billing/webhook`).
- Testing: JUnit 5 + `spring-boot-starter-test`, Testcontainers for
  Postgres, GreenMail for SMTP/IMAP integration tests.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   # @SpringBootApplication root
├── admin/       # /admin/revenue, churn/funnel metrics, weekly digest,
│                # win-back outreach (EPIC-18)
├── activation/  # onboarding activation drip email
├── auth/        # form + Google OIDC, account settings, password reset,
│                # email verification, login throttle
├── billing/     # Stripe checkout/portal/webhook, subscription lifecycle
├── digest/      # user-facing email digest opt-out
├── domain/      # JPA @Entity classes (EmailThread, Message, Participant,
│                # MailAccount, Subscription, Team, TeamInvite, …)
├── email/       # RFC 822 parsing, IMAP polling, IMAP IDLE
├── reengagement/# trial-end conversion / re-engagement drips
├── repository/  # Spring Data repositories
├── service/     # threading, IM transform (EmailBodyCleaner), etc.
├── team/        # team invites
└── web/         # Thymeleaf controllers, form objects, view models,
                 # PWA (manifest, sw.js, offline shell), SSE /chats/events
```

## Data model (schema)

`domain/` currently exposes: `Attachment`, `AuthEvent`, `AuthEventType`,
`AuthType`, `CancellationReason`, `DigestEmailPreference`, `EmailThread`,
`EmailVerificationToken`, `MailAccount`, `Message`, `MessageRecipient`,
`Participant`, `PasswordResetToken`, `Plan`, `PlanChangeEvent`,
`RecipientType`, `SavedSearch`, `Subscription`, `Team`, `TeamInvite`,
`User`, plus reengagement / activation tracking rows.

Threads are keyed by an RFC 822 `Message-ID` / `In-Reply-To` / `References`
chain; the `/chats` workspace additionally groups threads by a
`conversation_key` (participant-set) stored on `EmailThread` since V32.

## Auth model

Session-cookie based, browser-only:

- Form login: `POST /login` (BCrypt via `DbUserDetailsService`), CSRF on.
- Google OIDC: `GET /auth/google/start` → Spring Security's OAuth2 client
  handshake → `/login/oauth2/code/google`.
- All authenticated pages require a valid session — no token issuance,
  no `Authorization: Bearer …`, no OAuth resource server, no CORS
  allow-list for cross-origin clients.

## API surface (as of 2026-08-29)

**There is no REST API for a non-browser client.** All 22
`*Controller` classes are Thymeleaf `@Controller` classes returning
view names. Concrete exceptions:

- `BillingWebhookController` — the only `@RestController`. Consumes
  `POST /billing/webhook`, verifies Stripe signature, mutates
  subscription state. Not a general API.
- `ChatEventsController` — `GET /chats/events`, a server-sent-event
  stream authenticated by the browser session, driving in-place chat
  refresh in `chat.html`.
- `PwaController`, `SeoController` — static-ish assets (webmanifest,
  service worker, offline shell, robots, sitemap, OG card).

If a mobile client is added, this file and `docs/API.md` MUST be
updated first with the new contract — see `INTERNAL_TODO.md` at the
repo root.

## Scheduled jobs

- `MailboxPollingService` — IMAP inbox + sent-folder polling.
- `ImapIdleService` — IMAP IDLE for paid-tier accounts, gated by
  `mailbox.idle.enabled`.
- `ActivationService` scheduler — daily activation drip, gated by
  `ACTIVATION_ENABLED` / `ACTIVATION_CRON`.
- `TrialEndConversionService` — trial-end drip, gated by
  `TRIAL_END_ENABLED` / `TRIAL_END_CRON`.
- `AdminWeeklyDigestScheduler` — Monday operator digest, gated by
  `ADMIN_WEEKLY_DIGEST_ENABLED`.

## Website ↔ services

Thymeleaf controllers stay thin: they hand form input to a service in the
same package (or in `service/`), which owns validation and state
mutation. No business logic in templates. This layering is what any
future REST controller must reuse — never fork logic between web and API.

## Build & run

- `./mvnw clean package` — full build with tests.
- `./mvnw test` — tests only (H2, no Docker needed).
- `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
  — boot against local PostgreSQL 16 (see `db/init.sql`).

## Related repositories

- **`conexus-mail-mobile`** (sibling checkout): the mobile client is a
  greenfield Flutter project. It currently has no functional code and
  no server to talk to (see API-surface note above). See
  `../../conexus-mail-mobile/docs/DESIGN.md` for the mobile product
  intent (TBD until a human sets one), and `docs/API.md` here for the
  contract both repos must agree on.
