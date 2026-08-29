# Backend architecture — ConexusMail (email-messenger)

Spring Boot 3 monolith. Serves both the public website (Thymeleaf) and the
JSON REST API consumed by the `conexus-mail-mobile` Flutter client.

## Runtime

- Java 21, Maven (wrapper `./mvnw`).
- PostgreSQL 16 in prod and local dev; H2 for tests.
- Flyway owns the schema (`src/main/resources/db/migration/V*.sql`).
- Spring Data JPA + Hibernate for persistence (NOT jOOQ — the routine
  template's default was overridden years ago; see `pom.xml`).
- Thymeleaf views under `src/main/resources/templates/`.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/     — new-user activation nudges
├── admin/          — internal metrics, revenue, churn, weekly digest
├── api/            — JSON REST controllers (v1) consumed by the mobile client
├── auth/           — form login, Google OIDC, session, remember-me, password reset
├── billing/        — Stripe checkout/portal/webhooks, plan limits
├── digest/         — user-facing weekly digest email
├── domain/         — JPA entities + enums
├── email/          — IMAP polling / IDLE, MIME parsing, Gmail OAuth
├── reengagement/   — win-back / at-risk outreach
├── repository/     — Spring Data repositories
├── service/        — email→chat transform (bubbles, conversation keys, cleaner)
├── team/           — team invites and membership
└── web/            — Thymeleaf controllers and view models
src/main/resources/
├── templates/      — Thymeleaf views
├── static/         — CSS, JS, PWA manifest, icons
├── db/migration/   — Flyway migrations (V1__init.sql … V35__…)
└── application*.yml
```

## Auth model

Session-cookie based (`JSESSIONID` + optional `remember-me` cookie).
Configured in `auth/SecurityConfig.java`. Two entry paths:

1. Form login at `POST /login` (email + password, BCrypt).
2. Google OIDC at `GET /auth/google/start`, wired only when
   `GOOGLE_CLIENT_ID` is set (otherwise the whole `oauth2Login()` chain is
   skipped).

CSRF is on for every non-whitelisted mutating request. Exceptions:
`/h2-console/**`, `/billing/webhook` (Stripe posts a signed body, not a
CSRF token). See `SecurityConfig` for the full permit list.

The JSON REST API (`/api/v1/**`) is currently unauthenticated and exposes
only public read endpoints (health, version, plans). When user-scoped
endpoints are added they will use bearer-token or session-cookie auth —
that decision is deferred and tracked in `INTERNAL_TODO.md`.

## Website ↔ API discipline

Web pages and API endpoints MUST share the service layer. Never duplicate
business logic in a Thymeleaf controller vs. a REST controller. Today the
`api/` package is intentionally tiny and only surfaces read models; the
mature business logic lives in `service/`, `email/`, `billing/`, and
`auth/` and is reused by both call sites.

## Scheduled jobs

- `MailboxPollingScheduler` — periodic IMAP polling.
- `ImapIdleService` — IMAP IDLE for paid tiers (feature-flagged via
  `mailbox.idle.enabled`).
- `TrialEndConversionScheduler`, `WinBackOutreachService`,
  `AdminWeeklyDigestScheduler` — cron-style @Scheduled beans.

## Related repositories

- `conexus-mail-mobile` — Flutter client. See `docs/API.md` for the
  contract shared between the two repos and
  `../conexus-mail-mobile/docs/MOBILE.md` for the client architecture.
