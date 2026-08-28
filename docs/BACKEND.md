# Backend architecture

The backend is a monolith Spring Boot 3 web application on Java 21. It
owns both the ConexusMail SaaS website (Thymeleaf) and — once the
mobile client comes online — the JSON API a mobile client would use.
Today only the website exists; there is no mobile API yet (see
`API.md`).

## Tech

- Java 21, Spring Boot 3.x — `-web`, `-thymeleaf`, `-data-jpa`,
  `-validation`, `-mail`, `-security`, `-oauth2-client`.
- PostgreSQL 16 in dev and prod; H2 in tests. See `db/init.sql`.
- Flyway migrations in `src/main/resources/db/migration/` — 35 as of
  V35. Migrations are immutable once merged.
- Thymeleaf server-rendered views under
  `src/main/resources/templates/`.
- Jakarta Mail for IMAP/SMTP.
- JUnit 5 + Spring Boot Test, Testcontainers (Postgres), GreenMail.

Persistence uses **Spring Data JPA** (not jOOQ). The scheduled-routine
prompt talks about jOOQ; that is a generic template, not what this
project uses. If a future migration to jOOQ is desired, treat it as a
tracked roadmap item, not a silent change.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   # @SpringBootApplication entry
├── activation/                      # onboarding-nudge scheduler + email jobs
├── admin/                           # /admin/** operator dashboard, metrics services
├── auth/                            # form + Google OIDC auth, account settings,
│                                    # login throttle, email verification, password reset
├── billing/                         # Stripe checkout/portal/webhook + plan limits
├── digest/                          # weekly-digest email opt-out + scheduling
├── domain/                          # JPA entities (User, MailAccount, EmailThread,
│                                    # Message, Participant, Attachment, Subscription,
│                                    # Team*, SavedSearch, …)
├── email/                           # mailbox integration: IMAP poll (@Scheduled),
│                                    # IMAP IDLE for paid tiers, MIME parsing,
│                                    # Gmail OAuth client, credential encryption
├── reengagement/                    # win-back / follow-up email jobs
├── repository/                      # Spring Data JPA repositories
├── service/                         # conversation/IM-transform business logic:
│                                    # ConversationKeyService, IMTransformService,
│                                    # EmailBodyCleaner, ReplyService, …
├── team/                            # team invites + membership routes
└── web/                             # Thymeleaf controllers, forms, view models,
                                     # /chats/events SSE stream (ChatEventStream)
```

Package boundary: `web` and the feature-specific controller packages
(`auth`, `billing`, `team`, `digest`, `admin`) all funnel state changes
through `service/` / feature-service classes so behavior does not fork
between routes. A future mobile REST API MUST reuse those same service
classes — new controllers, same services.

## Runtime shape

- **HTTP entry.** Spring MVC. Every request is authenticated via
  Spring Security (`auth/SecurityConfig`) except a small allowlist of
  landing/legal/PWA/webhook routes. Sessions are cookie-based.
- **Background jobs.** `@Scheduled` in `email/MailboxPollingScheduler`
  polls mailboxes on the free tier; `email/ImapIdleService` holds an
  IDLE connection per paid-tier mailbox and publishes `NewMailEvent`
  on new mail. `admin/AdminWeeklyDigestScheduler`,
  `activation/*Scheduler`, and `reengagement/*Scheduler` fire timed
  email jobs.
- **Live push.** `web/ChatEventStream` fans `NewMailEvent`s out to
  browser tabs over Server-Sent Events at `GET /chats/events`
  (`text/event-stream`). Chat pages morph in place from these events
  (`fragments/live-refresh.html`).
- **Billing.** Stripe integration in `billing/`. The one
  `@RestController` in the whole codebase is
  `BillingWebhookController` which handles four Stripe events (see
  the root `README.md` table).

## Auth model

- **Passwords.** Argon2 (Spring Security default via
  `SecurityConfig`).
- **Google OIDC.** `spring-boot-starter-oauth2-client` + custom
  `GoogleOidcUserService` and `OAuth2ProvisioningService` provision a
  local `User` on first login, storing the Google subject on the row
  (V18).
- **Admin gate.** `AdminAuthorizer` checks the caller against a
  hardcoded allowlist in `application.yml` (`admin.emails`).
  Non-admins hitting `/admin/**` get a 404, not a 403 — the endpoint
  is invisible to non-operators.
- **Login throttle.** `LoginThrottleFilter` + `LoginThrottleService`
  clamp brute-force attempts per IP.

## Domain overview

See `CLAUDE.md` for the intent. The concrete entities live in
`com.emailmessenger.domain`. Threading is header-based
(`Message-ID` / `In-Reply-To` / `References`), and chat conversations
are keyed by the normalized participant set — see
`service/ConversationKeyService`. Any mobile client MUST derive the
same key or defer to the server for it.

## Testing

- `./mvnw test` runs the suite. H2 is used via
  `src/test/resources/application.yml`; Testcontainers-based tests
  auto-skip when Docker isn't available.
- New services should have unit tests in the mirrored package.
  Controllers get slice tests (`@WebMvcTest`).

## What the mobile client will need from us

See `API.md` for the current state of the mobile API contract (spoiler:
it doesn't exist yet). The concrete endpoints the mobile roadmap
depends on are listed as blocking items in
`../conexus-mail-mobile/INTERNAL_TODO.md`.
