# Backend Architecture

Spring Boot 3 (Java 21) application that serves both the ConexusMail
website (Thymeleaf) and — once implemented — the REST API consumed by
the `conexus-mail-mobile` Flutter client. The service layer is
authoritative for all state and business rules; web pages, JSON
endpoints, and the mobile client all funnel through the same services.

## Modules

Package root: `com.emailmessenger`.

| Package | Responsibility |
| --- | --- |
| `web/` | Thymeleaf controllers, view models, PWA endpoints, SSE stream at `/chats/events`. |
| `service/` | Conversation / bubble transformation, IM cleaner, reply composition, outbound mail. |
| `email/` | Mailbox integration: IMAP polling, IMAP IDLE, Gmail OAuth, outbound SMTP. |
| `auth/` | Form + Google OIDC login, password reset, email verification, account settings. |
| `billing/` | Stripe checkout, portal, webhook. |
| `team/` | Teams and invites. |
| `digest/` | Scheduled digest emails + opt-out. |
| `activation/`, `reengagement/` | Lifecycle nudges (onboarding, win-back). |
| `admin/` | Admin-only metrics and reconciliation. |
| `domain/` | JPA entities. |
| `repository/` | Spring Data repositories. |

## Persistence

- PostgreSQL 16 in dev and prod; H2 for tests via
  `src/test/resources/application.yml`.
- Flyway owns the schema. Migrations live under
  `src/main/resources/db/migration/V{n}__*.sql`. Never edit an applied
  migration — add a new one.
- Base config in `application.yml`; the `local` profile
  (`application-local.yml`) points at `localhost:5432/email_messenger`
  and reads `JDBC_DATABASE_URL/USERNAME/PASSWORD` env vars. `db/init.sql`
  bootstraps the database and app user.
- Entities are classes (constructor-injected). DTOs and view models are
  records.

## Domain model (selected)

- `User` — the owning account (form + Google OIDC).
- `MailAccount` — a connected mailbox (IMAP password OR Gmail OAuth),
  polling cursors, per-mailbox signature.
- `EmailThread` — thread grouping by participant-set
  `conversationKey`; carries owner + unread state.
- `Message` + `MessageRecipient` — parsed RFC 822 message with
  addresses; outbound-flag distinguishes user-sent messages.
- `Participant` — deduped by normalized email address.
- `Attachment` — filename, mime, size; `blob_ref` is currently unused
  (roadmap: attachment blob store + download endpoint).
- `SavedSearch`, `Team`, `TeamMember`, `TeamInvite`,
  `DigestEmailPreference`, `AuthEvent`, `Subscription`,
  `PlanChangeEvent` etc.

## Auth model

- Spring Security 6, form login + Google OIDC. See
  `auth/SecurityConfig.java` for the filter chain.
- Session-cookie based for the website. **The mobile client has no
  auth surface yet** — see `docs/API.md` for the planned token model.
- `AuthEventService` records logins, failed logins, and impersonation
  events for audit.

## Web (Thymeleaf) — how it relates to the API

The website and (future) REST API MUST share the service layer. Never
duplicate business rules in a controller or template. When a mobile
endpoint is added:

1. Add or reuse a method on a `*Service` class.
2. Add the JSON controller in the appropriate package (`web/api/` or
   equivalent), delegate to the service.
3. Add or update the Thymeleaf controller/template to call the same
   service.

Details of the website page inventory live in `docs/WEB.md`. The
mobile-facing REST contract lives in `docs/API.md`.

## Real-time delivery

- `MailboxPollingService` runs on a schedule; publishes `NewMailEvent`
  application events on new mail.
- `ImapIdleService` maintains IDLE connections for paid tiers
  (feature-flagged by `mailbox.idle.enabled`).
- `ChatEventStream` and `ChatEventsController` expose the events as an
  SSE stream at `/chats/events`. The mobile client will eventually
  consume the same events via a mobile-friendly transport (planned:
  websocket or push notification; see `docs/API.md`).

## Testing

- `./mvnw test` runs JUnit 5 + Spring Boot Test slices.
- Integration tests use Testcontainers (Postgres) and GreenMail
  (SMTP/IMAP) where required.

## Commands

- Build: `./mvnw clean package`
- Run locally:
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`

## Related repositories

- `conexus-mail-mobile` — Flutter mobile client. Consumes the REST
  contract defined in `docs/API.md`.
