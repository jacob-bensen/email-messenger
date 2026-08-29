# email-messenger — backend architecture

The backend is a Spring Boot 3 monolith. It serves **three** surfaces from
one deployment:

1. The **Thymeleaf website** at `/`, `/pricing`, `/chats`, `/mailboxes`, …
   (see `docs/WEB.md`).
2. The **JSON REST API** at `/api/v1/**` consumed by the
   `conexus-mail-mobile` Flutter client (see `docs/API.md`).
3. Background schedulers: mailbox polling, IMAP IDLE, digests, activation
   drip, weekly operator digest.

All three surfaces MUST go through the same service layer. No business
logic in controllers or Thymeleaf templates.

## Module map

| Package                                    | Role                                                                          |
|--------------------------------------------|-------------------------------------------------------------------------------|
| `com.emailmessenger`                       | `EmailMessengerApplication` — bootstrap                                       |
| `com.emailmessenger.api`                   | JSON REST controllers + DTO records used by the mobile client                 |
| `com.emailmessenger.web`                   | Thymeleaf controllers, view models, PWA & SEO endpoints                       |
| `com.emailmessenger.auth`                  | Spring Security config, form + Google OIDC login, account settings            |
| `com.emailmessenger.billing`               | Stripe checkout & webhook, plan limits, `PlanPricing`, `BillingPeriod`        |
| `com.emailmessenger.admin`                 | Operator dashboard (`/admin/revenue`), churn/MRR/at-risk/win-back services    |
| `com.emailmessenger.activation`            | Post-signup activation drip scheduler                                         |
| `com.emailmessenger.reengagement`          | Cold-mailbox reengagement                                                     |
| `com.emailmessenger.digest`                | Digest opt-out + delivery                                                     |
| `com.emailmessenger.team`                  | Team invites + acceptance                                                     |
| `com.emailmessenger.service`               | Domain services: email parsing, IM transform, conversation rendering          |
| `com.emailmessenger.email`                 | IMAP fetch / IDLE, SMTP send, provider adapters                               |
| `com.emailmessenger.domain`                | JPA entities                                                                  |
| `com.emailmessenger.repository`            | Spring Data JPA repositories                                                  |

## Persistence

- PostgreSQL 16 in prod + local dev (bootstrap: `db/init.sql`); H2 for tests.
- Schema migrations: **Flyway**, in `src/main/resources/db/migration/`.
  Migrations are immutable once merged (V1__…V35__… as of this doc). Add
  a new `V{n}__*.sql` rather than editing an existing one.
- Access via Spring Data JPA repositories; entities are classes, DTOs and
  view models are records.

## Auth model

- Session-cookie auth (Spring Security form login + remember-me), plus
  Google OIDC when `GOOGLE_CLIENT_ID` is set.
- CSRF is enabled for browser paths; `/h2-console/**` and `/billing/webhook`
  are exempted.
- **Public** URLs are whitelisted in `SecurityConfig.securityFilterChain`
  (landing, pricing, legal, PWA, `/api/v1/plans`). Everything else
  requires an authenticated session.
- The mobile app currently has **no dedicated auth path** — only the
  unauthenticated read-only `/api/v1/plans` endpoint exists. See
  `INTERNAL_TODO.md`.

## Website ↔ REST sharing rules

- Every route (web or REST) must call a `com.emailmessenger.service` /
  `.billing` / `.admin` service that owns the rule. Controllers only
  format input and output.
- Pricing figures on `/pricing`, in `AdminRevenueController`, and in
  `PlansApiController` all read from `com.emailmessenger.billing.PlanPricing`
  and `PlanLimits`. Do not fork these values into templates or DTO
  literals.
- All state-mutating flows run inside a single transaction (`@Transactional`
  on the service method) and never trust client-supplied values for
  anything the server can recompute.

## How to run

- Build: `./mvnw clean package`
- Local (needs `db/init.sql` applied to Postgres 16):
  `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Tests: `./mvnw test` (uses H2 + Testcontainers where marked)

## Related repositories

- **conexus-mail-mobile** — Flutter mobile client. Consumes `docs/API.md`;
  its `lib/domain/` mirrors this backend's rules.
