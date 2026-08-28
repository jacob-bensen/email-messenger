# BACKEND — email-messenger (ConexusMail)

Server-authoritative Spring Boot application. Serves both the public
Thymeleaf-rendered website and the JSON REST API used by the mobile
client (`conexus-mail-mobile`). All persistence and all business rules
live here. Clients (browsers, mobile) are not trusted for anything the
server can compute.

## Stack

- **Java 21** (LTS), **Spring Boot 3.5.x**
- **Persistence:** Spring Data **JPA** on **PostgreSQL 16** (H2 for
  tests). Schema is owned by **Flyway** — `src/main/resources/db/migration`.
- **Web:** `spring-boot-starter-web` + `-thymeleaf` for server-rendered
  pages; `@RestController` under `com.emailmessenger.api` for the mobile
  JSON API.
- **Security:** Spring Security form login (`/login`), remember-me,
  optional Google OIDC. BCrypt password hashing. CSRF is on for form
  posts; the JSON API is stateless and uses its own session-token
  header (see `docs/API.md`).
- **Mail:** Jakarta Mail for RFC 822 parsing; IMAP polling +
  `IMAP IDLE` for paid tiers; SMTP outbound via
  `spring-boot-starter-mail`.
- **Billing:** Stripe Checkout + Billing Portal via `stripe-java`.
- **Build:** Maven with wrapper. Tests: JUnit 5 + Spring Boot Test +
  Testcontainers + GreenMail.

Note: the routine's template calls for jOOQ; the actual code uses JPA.
Trust the code — do not migrate away from JPA absent a decision.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   # @SpringBootApplication
├── admin/                           # /admin/revenue, cohort/churn/win-back metrics
├── activation/                      # activation drip scheduler
├── api/                             # REST API for mobile client (JSON, stateless)
├── auth/                            # security, users, sessions, Google OIDC
├── billing/                         # Stripe checkout/portal/webhook, plan limits
├── digest/                          # weekly/monthly digest email
├── domain/                          # JPA entities + enums
├── email/                           # IMAP client, polling, IDLE, mailbox lifecycle
├── reengagement/                    # dormant-user re-engagement scheduler
├── repository/                      # Spring Data repositories
├── service/                         # IM transform, threading, chat model
├── team/                            # multi-user teams and invites
└── web/                             # Thymeleaf controllers, form/view models
```

`api/` is the boundary the mobile client depends on. Anything the
mobile app needs from a Thymeleaf controller MUST be lifted into
`service/` (or the existing service package) and re-exposed through an
`@RestController` under `api/` so the web and mobile paths share one
service layer. Never fork logic between `web/` and `api/`.

## Domain overview

Core entities:

- **User** — the account (email login, password hash / OIDC subject,
  plan, admin flag, timezone, appearance).
- **MailAccount** — a connected IMAP/Gmail mailbox owned by a User;
  encrypted credentials via `CredentialEncryptor`.
- **EmailThread** — RFC 822 threading by `Message-ID` /
  `In-Reply-To` / `References`; owned by a User (multi-tenant).
- **Message** — one email in a thread: sender + recipients (via
  `MessageRecipient`), plain + html body, sent-at, unread flag.
- **Participant** — deduped person by normalized email; display name +
  avatar.
- **Attachment** — filename, mime, size, blob ref (blob storage not
  yet implemented — see `INTERNAL_TODO.md`).
- **Subscription / PlanChangeEvent / CancellationReason** — Stripe
  subscription state and churn telemetry.
- **Team / TeamMember / TeamInvite** — shared workspaces.
- **SavedSearch** — persisted queries.
- **DigestEmailPreference** — per-user weekly digest opt-out.
- **AuthEvent** — login audit trail (success, failure, throttle).

Grouping and view models (in `service/`):

- **ConversationKey** — a stable slug for a participant set;
  conversations are threads sharing the same key. Produced by
  `ConversationKeyService`.
- **ChatConversation / BubbleRun / BubbleMessage** — the IM-style
  timeline model rendered by both the Thymeleaf chat view and (soon)
  the mobile client.

## Auth model

- **Website (Thymeleaf):** stateful HTTP session (cookie
  `JSESSIONID`), form login at `POST /login`, remember-me token, CSRF
  protection.
- **Mobile client:** stateless. Uses `POST /api/auth/login`
  (email+password) to obtain an **API session token** returned as a
  bearer credential; every subsequent `/api/**` call sends
  `Authorization: Bearer <token>` and `Cookie: JSESSIONID=<...>` is
  ignored on `/api/**`. Tokens are opaque, server-generated, revocable
  from `/account`. Details: `docs/API.md`.

The API path is CSRF-exempt (stateless), rate-limited by the same
`LoginThrottleService` as form login, and MUST reject any request
without a valid bearer token.

## Web ↔ API rule

- All state mutations run through a single `service/` method wrapped
  in a Spring transaction (read → validate → mutate → audit).
- A Thymeleaf controller and its REST counterpart both call the same
  service method with the same arguments; the only difference is
  request/response marshalling.
- The server recomputes everything security- or economy-relevant
  (plan limits, participant sets, ownership). The client is never
  trusted for those.

## Migrations

Flyway migrations under `src/main/resources/db/migration` are
**immutable once merged**. Add a new `V{n}__*.sql` rather than
editing an existing one. Current head is around `V35`.

## Related repositories

- **conexus-mail-mobile** — Flutter mobile client. Its `docs/DESIGN.md`
  is the product roadmap SSoT; its `docs/MOBILE.md` describes the
  client architecture. Both repos share `docs/API.md` (canonical in
  THIS repo).
