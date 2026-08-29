# Backend Architecture

Spring Boot 3.5.x application (Java 21, Maven) that is both the marketing /
web app *and* the REST API for the ConexusMail mobile client. Package root
is `com.emailmessenger`.

## Module layout

```
com.emailmessenger
├── EmailMessengerApplication.java  # Spring Boot entrypoint (@SpringBootApplication)
├── activation/                     # Onboarding email drip
├── admin/                          # /admin/*: metrics, retention queue, digests
├── api/                            # REST controllers for the mobile client (new)
├── auth/                           # Form login, Google OIDC, sessions
├── billing/                        # Stripe products/prices, webhooks, portal
├── digest/                         # Weekly digest emails to end users
├── domain/                         # JPA entities (User, MailAccount, Message, ...)
├── email/                          # IMAP polling, IDLE, GreenMail-compat glue
├── reengagement/                   # Trial-end + re-engagement mail
├── repository/                     # Spring Data repositories
├── service/                        # Chat conversation, IM transform, body cleaner
├── team/                           # Team + invitations
└── web/                            # Thymeleaf controllers (site + chats UI)
```

## Data

- PostgreSQL 16 in production and local dev (see `db/init.sql`), H2 for tests.
- Persistence is **Spring Data JPA + Hibernate** (not jOOQ). Entities live in
  `domain/`, repositories in `repository/`.
- Schema is managed by **Flyway**; migrations under
  `src/main/resources/db/migration/V{n}__*.sql`. Migrations are immutable once
  merged — add a new `V{n+1}` file rather than editing an existing one.

## Auth model

Two authentication paths share the same `User` row:
- **Form login** — email + bcrypt password via
  `SecurityConfig`/`UserDetailsService` (session cookie).
- **Google OIDC** — Spring Security OAuth2 client
  (`AUTH_GOOGLE_CLIENT_ID`/`_SECRET`).
- All in-app pages (Thymeleaf under `/chats`, `/mailboxes`, `/account`,
  `/admin`, …) are session-authenticated.
- The REST API under `/api/**` is currently **anonymous** — only endpoints
  that reveal no user data (`/api/v1/app-info`) are exposed. When the first
  authenticated REST endpoint lands, it will use a bearer-token scheme
  documented in `API.md`.

## Web (Thymeleaf) vs REST (mobile)

The server is authoritative for **all** persistence and business rules. Both
the Thymeleaf website and the mobile REST API MUST reach state through the
same service layer under `com.emailmessenger.service` /
`com.emailmessenger.*` — never duplicate business logic into a controller
or a template. Web-only concerns (view models, `SiteModelAdvice`,
`NavModelAdvice`, fragments) live in `web/`; JSON DTOs and REST-specific
mapping live in `api/`.

See `WEB.md` for the Thymeleaf page inventory and `API.md` for the REST
contract.

## Configuration

- `src/main/resources/application.yml` — base config, reads
  `JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`.
- `application-local.yml` (`local` profile) — points at
  `localhost:5432/email_messenger`.
- `application-prod.yml` — production overrides.
- Env-driven feature flags gate schedulers (`ACTIVATION_ENABLED`,
  `TRIAL_END_ENABLED`, `ADMIN_WEEKLY_DIGEST_ENABLED`, `mailbox.idle.enabled`).

## Tests

- `./mvnw test` — unit + slice tests use H2 via
  `src/test/resources/application.yml`.
- `./mvnw verify` — same, wired for CI-shaped runs.
- Integration tests that need Postgres + IMAP use Testcontainers + GreenMail.

## Related

- Mobile client: `jacob-bensen/conexus-mail-mobile` — see `docs/MOBILE.md`
  and `docs/DESIGN.md` in that repo.
- API contract: `docs/API.md` in this repo is the single source of truth
  for the mobile-facing REST surface.
