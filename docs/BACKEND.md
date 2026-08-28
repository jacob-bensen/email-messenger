# BACKEND — ConexusMail (email-messenger)

Authoritative Spring Boot service. Owns the database, all business logic,
the Thymeleaf website, and (starting now) a REST API for the
`conexus-mail-mobile` Flutter client.

## Related repositories

- Sibling: [`conexus-mail-mobile`](../../conexus-mail-mobile) — mobile
  client that consumes the API defined in `docs/API.md`.
- Contract: `docs/API.md` in this repo is the single source of truth for
  every request/response the mobile client can send.

## Stack

- Java 21, Spring Boot 3, Spring MVC + Thymeleaf, Spring Security,
  Spring Data JPA
- PostgreSQL 16 in prod / local; H2 in tests
- Flyway (migrations under `src/main/resources/db/migration/`, currently
  `V1` – `V35`)
- Jakarta Mail (IMAP + SMTP)
- Maven wrapper (`./mvnw`)

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/       # onboarding drip + activation
├── admin/            # /admin/revenue, churn + win-back
├── auth/             # sessions, form + Google OIDC, remember-me
├── billing/          # Stripe checkout + webhook
├── digest/           # weekly digests, opt-out
├── domain/           # JPA entities
├── email/            # IMAP polling, IDLE, SMTP send
├── reengagement/     # trial-end + activation cadence
├── repository/       # Spring Data repositories
├── service/          # ChatService, ConversationService, IMTransformService, …
├── team/             # teams + invites
└── web/              # Thymeleaf controllers, form objects, SSE stream
```

**Web controllers vs REST**: today every controller except
`BillingWebhookController` is a Thymeleaf controller (`@Controller`
returning template names). The new mobile API lives under `web.api`
(planned) and uses `@RestController`. Both call the **same service
layer** — never fork business rules between HTML and JSON paths.

## Domain overview

Core entities (see `com.emailmessenger.domain.*`):

- `User` — auth principal; owns mail accounts and subscription.
- `MailAccount` — a connected mailbox (IMAP password or Gmail OAuth).
  Encrypted secrets via `email.CredentialEncryptor`; polling cursor per
  folder (`V6`, `V31`).
- `EmailThread` — RFC 822 threading (`Message-ID` / `In-Reply-To` /
  `References`). Now carries a `conversationKey` (`V32`) keyed by
  participant-set so cross-thread bubbles collapse into one chat.
- `Message` + `MessageRecipient` + `Attachment` — one email row per
  imported message; recipients are `TO/CC/BCC` typed.
- `Participant` — deduped by normalized address; display name +
  optional avatar.
- Billing / churn: `Subscription`, `Plan`, `PlanChangeEvent`,
  `CancellationReason`, digest / win-back flags.
- `Team`, `TeamMember`, `TeamInvite`, `SavedSearch`.

## Auth model

Session-based, cookie auth via Spring Security (`auth/SecurityConfig.java`):

- Form login at `POST /login` (email / password).
- Optional Google OIDC login when `AUTH_GOOGLE_CLIENT_ID` +
  `AUTH_GOOGLE_CLIENT_SECRET` are set (wired lazily by
  `SecurityConfig`).
- Remember-me via `PersistentTokenRepository` (JDBC-backed, 30 days).
- Public URLs are whitelisted in `SecurityConfig`; everything else needs
  auth.

**Mobile API auth (planned)**: the mobile client cannot follow form
redirects the way a browser can. The plan (see `docs/API.md#auth`) is to
issue an opaque bearer token from `POST /api/v1/auth/session`, back it
with the same `User` row, and pass it as `Authorization: Bearer …`. This
adds an `api_session` table + a Spring Security filter that resolves the
token to a `UsernamePasswordAuthenticationToken` before controllers see
the request. Until that lands, the mobile client uses **only unauth
endpoints** (`GET /api/v1/version`, health).

## How the website relates to the API

Thymeleaf pages render server-side and post form data back — nothing
JSON. Every mutating action (send reply, connect mailbox, mark read,
save search) runs through a service class in `service/`, `email/`,
`billing/`, etc. The REST controllers we add for the mobile client
**delegate to those same services**; controllers are thin adapters that
translate between HTTP JSON and service methods.

Do not duplicate rules in `@Controller` or Thymeleaf templates. If a
web page needs data-shaping, put it in a service and call it from both
paths.

## Running locally

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Requires local PostgreSQL 16; bootstrap with `db/init.sql`. Tests use
H2 automatically (`./mvnw test`). No Docker, no CI — decisions from
2026-06-11 in the roadmap.

## Where to look next

- `docs/API.md` — the mobile API contract.
- `docs/WEB.md` — Thymeleaf page inventory.
- `CLAUDE.md` — build / test commands + roadmap.
- `INTERNAL_TODO.md` — running list of gaps and contract drift.
