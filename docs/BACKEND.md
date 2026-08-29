# Backend Architecture

## What ships here

One Spring Boot app that serves both:

1. The **public website** — Thymeleaf-rendered pages under
   `src/main/resources/templates/`. Users land on `/`, sign up, connect
   mailboxes, and use the `/chats` workspace here. See `docs/WEB.md`.
2. The **mobile REST API** at `/api/v1/**`, consumed by the
   `conexus-mail-mobile` Flutter client. See `docs/API.md`.

Both go through the SAME service layer. Do not fork business logic into
controllers or templates — a Thymeleaf page and a REST endpoint that
mutate the same state must call the same service method.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/       # onboarding / activation nudges
├── admin/            # admin metrics dashboard
├── auth/             # Spring Security, form login, Google OIDC, sessions
├── billing/          # Stripe integration, plan enforcement
├── digest/           # daily/weekly email digests
├── domain/           # JPA entities (User, EmailThread, Message, …)
├── email/            # IMAP/SMTP integration, mailbox polling, IMAP IDLE
├── reengagement/     # win-back emails
├── repository/       # Spring Data repositories
├── service/          # email parsing, IM transform, threading
├── team/             # teams & invites
└── web/              # Thymeleaf controllers + view models
    └── api/          # NEW: REST controllers for the mobile client
```

## Auth model

- Form login (email + password) + "Continue with Google" (OIDC).
- Sessions stored server-side; the browser and the mobile client both
  carry the `JSESSIONID` cookie.
- `remember-me` uses `JdbcTokenRepositoryImpl` (see `SecurityConfig`).
- CSRF is enabled globally; `/api/v1/**`, the Stripe webhook, and the
  H2 console are exempt.
- Password hashing: BCrypt via `PasswordEncoder` bean.
- Login throttling: `LoginThrottleFilter` runs before the form-login
  filter so a locked email never reaches credentials check.

## Persistence

- PostgreSQL 16 in prod and local dev; H2 in tests
  (`src/test/resources/application.yml`).
- Schema managed by Flyway (`src/main/resources/db/migration/`).
  Migrations are **immutable once merged** — add `V{n+1}__*.sql` rather
  than editing an existing one. As of 2026-08-29 we're at `V35`.
- Spring Data JPA repositories in `com.emailmessenger.repository`.
  `spring.jpa.hibernate.ddl-auto=validate`; Flyway is authoritative.

## Live delivery

- Paid-tier accounts use `ImapIdleService` (feature-flagged by
  `mailbox.idle.enabled`).
- Web clients get an SSE stream at `/chats/events` from `ChatEventStream`;
  new mail is published as `NewMailEvent` by `MailboxPollingService`.
- Mobile equivalent (`/api/v1/chats/events`) is on the ROADMAP but not
  yet implemented.

## Configuration

- Base `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars.
- `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`; bootstrap with `db/init.sql`.
- Google OAuth wires in only when `GOOGLE_CLIENT_ID` is set (see
  `SecurityConfig#securityFilterChain`).
- Mail: `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASS`
  (defaults to `localhost:587`).

## Build & test

- Build: `./mvnw clean package`
- Run local: `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test all: `./mvnw test`
- One test: `./mvnw test -Dtest=ClassName#method`

Integration tests use Testcontainers (Postgres) and GreenMail
(SMTP/IMAP); slice tests use `@WebMvcTest` / `@SpringBootTest` +
`AutoConfigureMockMvc`.

## Conventions (repeated for prominence)

- Constructor injection only; no field `@Autowired`.
- Records for DTOs and view models; entities are classes.
- Package-private by default.
- Tests mirror the package of the class under test in
  `src/test/java/...`.
- Don't add comments that just restate the code.
- No backwards-compatibility shims while the project is pre-1.0.
