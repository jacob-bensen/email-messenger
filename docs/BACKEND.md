# Backend architecture

Spring Boot 3 monolith. Serves three surfaces:

1. **Public marketing + auth pages** (Thymeleaf) — see `docs/WEB.md`.
2. **In-app "chats" workspace** (Thymeleaf) — server-rendered app.
3. **JSON API for the mobile client** at `/api/v1/**` — see `docs/API.md`.

All three go through the same service layer. There is no separate BFF and
no duplicated business logic between web pages and API endpoints — a
controller is a thin adapter.

## Modules

Package tree under `com.emailmessenger`:

- `activation/` — new-user activation nudges.
- `admin/` — internal admin dashboards (revenue, weekly digest, win-back).
- `api/v1/` — JSON API for the mobile client.
- `auth/` — form login, Google OIDC, password reset, email verification,
  session management, security config.
- `billing/` — Stripe integration: checkout, portal, webhook, plan changes.
- `digest/` — user-facing digest emails + opt-out.
- `domain/` — JPA entities (records-for-DTOs convention; entities are classes).
- `email/` — IMAP polling, Gmail OAuth, IDLE, outbound SMTP.
- `reengagement/` — dormant-user re-engagement outreach.
- `repository/` — Spring Data repositories.
- `service/` — cross-cutting business services (reply threading,
  conversation transform, quoted-reply stripping, chat event stream).
- `team/` — teams, invites.
- `web/` — Thymeleaf controllers, forms, view models.

## Persistence

- PostgreSQL 16 in dev + prod (H2 in tests via `src/test/resources/application.yml`).
- Flyway migrations under `src/main/resources/db/migration/`; **immutable
  once merged**. New change → new `V{n}__*.sql`.
- Spring Data JPA repositories under `com.emailmessenger.repository`.
- `spring.jpa.hibernate.ddl-auto: validate` — Flyway owns the schema.

## Auth model

- Sessions are cookie-based (`JSESSIONID` + remember-me token).
- Two credentials paths: form login (BCrypt) and Google OIDC.
- Login is rate-limited via `LoginThrottleFilter` (runs before
  `UsernamePasswordAuthenticationFilter`).
- CSRF is on by default; exemptions: `/h2-console/**`, `/billing/webhook`,
  `/api/v1/**` (the API is stateless and will use bearer tokens).
- The API bearer-token layer is **not implemented yet**; today the mobile
  client can only reach unauthenticated endpoints (`/api/v1/health`).

## Live delivery

- IMAP IDLE runs for paid-tier accounts (`ImapIdleService`, flag
  `mailbox.idle.enabled`).
- New-message events publish to the SSE stream at `/chats/events`
  (`ChatEventStream`, `NewMailEvent`).
- The mobile client will consume the same event stream once the API
  auth path lands.

## Related repositories

- **conexus-mail-mobile** — Flutter mobile client. Consumes `docs/API.md`.
  Any change to a payload or path in this file MUST be applied to that
  repo's networking layer in the same run.
