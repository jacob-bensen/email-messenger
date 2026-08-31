# Backend architecture (`email-messenger`)

Spring Boot 3 / Java 21 monolith. It owns:

- The **REST API** at `/api/v1/**` for the Flutter mobile client
  (`conexus-mail-mobile`). See `docs/API.md` for the wire contract.
- The **public website + product web app** at `/**` — Thymeleaf pages for
  marketing, auth, /chats, billing, admin. See `docs/WEB.md`.
- All persistence (PostgreSQL 16, JPA/Hibernate, Flyway migrations).
- All business logic. Both the REST API and the website go through the
  same service layer — logic must never live in a controller alone.

## Package layout (`com.emailmessenger.*`)

```
EmailMessengerApplication.java   – Spring Boot entry point, Clock bean
api/                             – REST controllers (/api/v1/**), JSON DTOs
web/                             – Thymeleaf controllers + view models + form objects
service/                         – Business logic; called by both api/ and web/
domain/                          – JPA entities (User, Message, EmailThread, …)
repository/                      – Spring Data JPA repositories
email/                           – IMAP polling, IMAP IDLE, Jakarta Mail glue
auth/                            – SecurityConfig, form login, OIDC, throttling
billing/                         – Stripe integration + webhook
admin/                           – Revenue/retention admin views
activation/                      – Onboarding nudge emails
reengagement/                    – Win-back emails
team/                            – Team invites
digest/                          – Daily digest emails
```

**Convention.** Controllers (whether under `api/` or `web/`) are the
adapter layer. They validate input, delegate to a service, and marshal
the result. All state mutation runs in a single service-layer transaction
so the mobile API and the website behave identically.

## Data flow at a glance

```
IMAP server ──► email/MailboxPollingService ──► service/EmailImportService ──► domain/*
                            │                          │
                            └── IMAP IDLE ──► NewMailEvent ──► web/ChatEventStream (SSE)
                                                                       │
                                                                       └► /chats/events (browser)

Browser  ──► web/*Controller ──► service/*  ──► repository/*  ──► PostgreSQL
Mobile   ──► api/*Controller ──► service/*  ──► repository/*  ──► PostgreSQL
```

The mobile push/live-mail path is not yet wired — see
`INTERNAL_TODO.md`.

## Domain model (short)

- `User` — email, bcrypt hash (or Google OIDC subject), display name,
  default `MailAccount`, sync frequency, several activation timestamps.
- `MailAccount` — IMAP host/port/user, encrypted password OR OAuth refresh
  token (`authType` enum), polling cursors (`lastSeenUid`,
  `lastSeenSentUid`), failure counters, signature.
- `EmailThread` — owner, subject, root Message-ID, `conversationKey`
  (SHA-256 of the normalized participant set), unread flag.
- `Message` — inside a thread: headers, plain+html body, sender, list of
  `MessageRecipient` (TO/CC/BCC), attachments, `outbound` flag.
- `Participant` — deduped by normalized email address.
- `Attachment` — filename, mime, size. `blobRef` currently null; blob
  storage is a roadmap item in the top-level `CLAUDE.md`.
- `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`,
  `Subscription`/`Plan`/`PlanChangeEvent`, auth tokens.

See `src/main/java/com/emailmessenger/domain/` for the full field list.

## Persistence

- Flyway migrations under `src/main/resources/db/migration/V*.sql`.
- **Immutable once merged** — a change is always a new `V{n}__*.sql`.
- Local dev: `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`; bootstrap with `db/init.sql`.
- Tests: H2 via `src/test/resources/application.yml`.

## Auth (as of v0.1)

- **Website**: Spring Security form login (`POST /login`, username field
  `email`) + Google OIDC when `GOOGLE_CLIENT_ID` is set. Sessions are
  cookie-based (`JSESSIONID`) with a persistent `remember-me` token.
  CSRF is globally enabled; only `/h2-console/**`, `/billing/webhook`,
  and `/api/**` are exempt.
- **Mobile API**: `/api/v1/health` is public. Every other `/api/v1/**`
  endpoint (once it exists) will require a bearer token. Token issuance
  isn't wired yet — see `INTERNAL_TODO.md`.

## Real-time delivery

- IMAP IDLE (`email/ImapIdleService`, flag `mailbox.idle.enabled`) on
  paid-tier accounts publishes a `NewMailEvent` after import commit.
- Browser tabs subscribe via `GET /chats/events` (SSE, session-cookie
  auth). Mobile has no equivalent yet — planned as WebSocket or SSE with
  bearer-token auth.

## Testing

- `./mvnw test` runs the JUnit 5 + Spring Boot Test suite.
- Slice tests use `@SpringBootTest` with `MockitoBean` (see
  `api/ApiHealthControllerTest.java` for the pattern).
- Integration tests use Testcontainers (PostgreSQL) + GreenMail
  (in-process IMAP/SMTP).

## Related repositories

- **`conexus-mail-mobile`** — Flutter mobile client. Talks to `/api/v1`.
  Contract lives in `docs/API.md` (this repo). Product roadmap lives in
  `conexus-mail-mobile/docs/DESIGN.md`.
