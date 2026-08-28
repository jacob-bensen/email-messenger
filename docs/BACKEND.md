# BACKEND

Spring Boot 3 application that serves both the ConexusMail website
(Thymeleaf, session cookie auth) and — going forward — a JSON REST API
for the ConexusMail mobile client (`../conexus-mail-mobile`).

The backend is the sole source of truth for all persisted data and all
business rules. The mobile client mirrors selected pure-logic rules in
its `lib/domain/` for optimistic prediction, but never trusts its own
computation over a server response.

## Runtime

- Java 21, Spring Boot 3.5.x, Maven (with wrapper).
- PostgreSQL 16 in production and local dev; H2 in tests
  (`src/test/resources/application.yml`).
- Flyway migrations under `src/main/resources/db/migration`, immutable
  once merged. Current head: `V35` (see `db/migration/` for the full
  list).
- JPA / Hibernate for persistence (NOT jOOQ; the generic routine's
  assumed stack is inaccurate for this project — no jOOQ codegen step
  exists).
- Default HTTP port: 8080.

## Package layout (`com.emailmessenger.*`)

| package        | responsibility |
| -------------- | --------------- |
| `web`          | Thymeleaf MVC controllers, form objects, view models, PWA shell |
| `auth`         | Spring Security config, form-login + Google OIDC, throttling, verification tokens, password reset |
| `billing`      | Stripe checkout, portal, webhook, plan gating |
| `domain`       | JPA entities (`User`, `Subscription`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Team`, `SavedSearch`, `Plan`, etc.) |
| `repository`   | Spring Data JPA repositories |
| `service`      | Email parsing, IM transform, threading, conversation projection, reply composition |
| `email`        | IMAP/SMTP integration: polling job, IDLE listener, OAuth token refresh, outbound send |
| `team`         | Team invites and membership |
| `digest`       | Weekly / cadence digests |
| `activation`   | New-user activation and re-engagement drips |
| `reengagement` | Long-idle re-engagement outreach |
| `admin`        | Operator dashboards (`/admin/revenue`), retention queue, win-back sends |

The `EmailMessengerApplication` class boots everything.

## Auth model

- Web browsers: form login (`/login`) with a `JSESSIONID` cookie plus
  optional `remember-me` cookie. Google OIDC when the client-id env is
  set.
- Passwords: BCrypt via `PasswordEncoder`.
- CSRF: enabled by default (Thymeleaf pages include the token
  automatically). Exempted for Stripe webhooks and the H2 console.
- Mobile clients (planned, not yet implemented): the current API surface
  is anonymous (`/api/v1/health`, `/api/v1/version`). Once mobile begins
  reading user data, session bearer tokens will be issued through a
  dedicated `POST /api/v1/session` endpoint (see `docs/API.md`
  §Authentication for the tracked plan).

## Website / API relationship

The Thymeleaf site (`docs/WEB.md`) and the REST API (`docs/API.md`) MUST
share the service layer — controllers on either side call the same
`*Service` classes, which own all validation and persistence. No
business logic in controllers, none in templates, none duplicated across
the two surfaces.

Because the current REST API is minimal, the enforceable rule for this
run is: any service method a REST controller calls must be the same
method its Thymeleaf sibling already calls — no forked "API-only"
implementation. Follow-up work adding mobile-facing endpoints (auth,
conversations, mailboxes) will re-use `AccountService`, `ChatService`,
`ConversationListService`, `MailboxController`'s services, and
`OutboundMessageService` unchanged.

## Local run

Requires local PostgreSQL 16 with schema/user bootstrapped from
`db/init.sql`:

```
JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local
```

Tests use H2 and need no external services:

```
./mvnw test
```

## Related repositories

- **`../conexus-mail-mobile`** — Flutter mobile client. Consumes the
  REST API defined in `docs/API.md`. Mirrors pure-logic rules from this
  repo in its `lib/domain/`.
