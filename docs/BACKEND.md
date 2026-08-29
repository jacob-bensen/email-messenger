# Backend architecture

Spring Boot 3.5 on Java 21. This single service backs three surfaces:

1. **Website** — Thymeleaf-rendered pages under `/` (marketing, auth,
   billing, `/chats` texting-style workspace, `/admin/*` operator views).
   See [`WEB.md`](./WEB.md).
2. **Mobile API** — JSON endpoints under `/api/*` consumed by
   `conexus-mail-mobile`. See [`API.md`](./API.md).
3. **Operational endpoints** — `/actuator/health` (liveness/readiness),
   `/billing/webhook` (Stripe), scheduled jobs (`@EnableScheduling`).

The three surfaces share the SAME service layer (`com.emailmessenger.*`
service classes). Business logic never lives in a Thymeleaf controller,
a REST controller, or a scheduler — they call into services.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   # @SpringBootApplication + Clock bean
├── api/                             # JSON REST controllers (/api/**)
├── web/                             # Thymeleaf controllers (/, /chats, /account)
├── admin/                           # /admin/* operator views + services
├── auth/                            # Spring Security, form-login, Google OIDC
├── billing/                         # Stripe: checkout, portal, webhook
├── digest/                          # weekly / monday operator digests
├── activation/                      # activation drip, trial-end conversion
├── reengagement/                    # win-back outreach
├── team/                            # team + invites
├── email/                           # IMAP/SMTP integration, polling, IDLE
├── service/                         # conversation model, threading, reply
├── domain/                          # JPA entities
└── repository/                      # Spring Data JPA repositories
src/main/resources/
├── templates/                       # Thymeleaf views
├── static/                          # CSS, JS, images, PWA icons
├── db/migration/                    # Flyway migrations (V1..VN)
└── application*.yml
```

## Persistence

- **Runtime.** PostgreSQL 16, via `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars.
- **Tests.** H2 in-memory via `src/test/resources/application.yml`.
- **Migrations.** Flyway; add `V{n}__*.sql` — never edit an applied
  migration (repeatable rule from `CLAUDE.md`).
- **ORM.** Spring Data JPA. Records for DTOs and view models; classes for
  entities; constructor injection everywhere.

## Auth model

- Session cookie (`JSESSIONID`) is the sole auth token today, shared
  between website and mobile API.
- Form-login at `POST /login`; `LoginThrottleFilter` runs before the
  form-login filter to lock out brute force by email.
- Google OIDC is wired only when `AUTH_GOOGLE_CLIENT_ID` is set; without
  it the "Continue with Google" flow is silently omitted.
- `SecurityConfig.securityFilterChain` permits `/api/health` anonymously
  and exempts `/api/**` from CSRF. Every OTHER `/api/*` endpoint MUST
  authenticate its caller explicitly (e.g., `@PreAuthorize`, `Principal`
  check).

## Scheduling

`@EnableScheduling` on the application class. Every scheduled job is
gated behind a feature flag env var (`ACTIVATION_ENABLED`,
`TRIAL_END_ENABLED`, `ADMIN_WEEKLY_DIGEST_ENABLED`,
`MAILBOX_IDLE_ENABLED`, …) so the tests and dev environments keep the
service beans available for direct invocation without the schedulers
firing.

## Real-time

- IMAP IDLE for paid mailboxes → `NewMailEvent` → Server-Sent Events at
  `/chats/events` — feeds live in-place chat updates for the website.
- The mobile API has NO real-time push yet. When it does, prefer SSE
  under `/api/events` to keep the mobile transport symmetric with the
  website.

## Testing

- `./mvnw test` — full suite; Spring Boot slice tests + Testcontainers +
  GreenMail SMTP/IMAP for integration.
- New controllers under `com.emailmessenger.api` mirror to
  `src/test/java/com/emailmessenger/api/*Test.java` and use
  `@SpringBootTest @AutoConfigureMockMvc` with `@MockitoBean` for heavy
  collaborators (see `ApiHealthControllerTest`).
