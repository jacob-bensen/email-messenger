# Backend architecture — email-messenger (ConexusMail)

Java 21, Spring Boot 3.5.x, PostgreSQL 16 (H2 in tests), Flyway,
Thymeleaf, Spring Security, Spring Data JPA. Maven wrapper. Runs
locally against a Postgres bootstrapped by `db/init.sql`.

The single application serves two audiences:

1. **The ConexusMail marketing website + web app** — server-rendered
   Thymeleaf pages at `/`, `/login`, `/chats/**`, `/mailboxes/**`, etc.
   Described in `docs/WEB.md`.
2. **The ConexusMail mobile client** — REST endpoints under
   `/api/v1/**`. Described in `docs/API.md`. Currently only the
   smoke-test endpoint exists; the rest of the API is planned.

Both audiences MUST call the same service layer (`service/`) for any
business logic. Never duplicate rules between a controller and a
template — that is how mobile and web drift apart.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java     # Spring Boot entry, Clock bean, @EnableAsync/Scheduling
├── api/                                # REST controllers for /api/v1/**  (mobile)
├── web/                                # Thymeleaf controllers + form/view models  (website)
├── auth/                               # Spring Security config, form + Google OIDC login,
│                                       # email verification, password reset, throttle filter
├── billing/                            # Stripe integration + webhook, plan gating
├── admin/                              # Internal admin metrics/pages
├── digest/                             # Weekly digest emails + opt-out
├── activation/                         # Activation-nudge emails for new users
├── reengagement/                       # Win-back / re-engagement email jobs
├── team/                               # Teams + invite management
├── service/                            # Business logic: IM transform, threading, replies,
│                                       # conversation keys, quoted-reply stripping
├── email/                              # IMAP/SMTP: MailAccountService, MailboxPollingService,
│                                       # ImapIdleService, OwnerAddressService, NewMailEvent
├── domain/                             # JPA entities (User, EmailThread, Message, ...)
└── repository/                         # Spring Data JPA repositories
```

## Persistence

- Schema is managed exclusively via Flyway migrations under
  `src/main/resources/db/migration/V*.sql`. Migrations are **immutable
  once merged** — add a new `V{n+1}__desc.sql` rather than editing.
- Tests use H2 (see `src/test/resources/application.yml`); production
  and local dev use PostgreSQL 16.

Key entities:

- `User` — account, plan, auth type (form/Google), email-verification
  state.
- `MailAccount` — one connected mailbox (IMAP+SMTP, either password or
  Gmail OAuth). Carries polling policy, last-seen UIDs, signature.
- `EmailThread` — grouped Messages, keyed by `Message-ID`/`In-Reply-To`
  and (since V32) a `conversationKey` derived from the participant set.
- `Message`, `MessageRecipient`, `Participant`, `Attachment` — one
  RFC-822 message and its people/files.
- `Subscription`, `Plan`, `PlanChangeEvent` — Stripe subscription state
  and history.
- `Team`, `TeamMember`, `TeamInvite` — team scoping.
- `SavedSearch`, `DigestEmailPreference`, `AuthEvent`,
  `PasswordResetToken`, `EmailVerificationToken` — misc.

## Auth

`SecurityConfig` wires:

- Form login at `/login` (email + password + optional remember-me).
- Google OIDC — activated only when both `GOOGLE_CLIENT_ID` and
  `GOOGLE_CLIENT_SECRET` env vars are set (the OAuth registrations bean
  is missing otherwise).
- Public routes (landing, pricing, legal, PWA assets, actuator health,
  `/api/v1/version`) permitted; everything else requires
  authentication.
- CSRF stays on; exempted paths: `/h2-console/**`, `/billing/webhook`,
  and the whole `/api/**` block (mobile uses bearer tokens once auth
  lands).

Mobile-facing auth is TBD — see `docs/API.md`.

## Mail import & live delivery

- `MailboxPollingService` runs on a `@Scheduled` cadence, fetches new
  INBOX + Sent-folder messages via IMAP, and threads them.
- `ImapIdleService` (paid tier, `mailbox.idle.enabled`) holds IDLE
  connections open and publishes `NewMailEvent`s on new mail.
- `ChatEventStream` fans those events out as SSE at `/chats/events` for
  the web PWA. The mobile client will consume a parallel
  `/api/v1/events` stream once implemented.

## IM transform

The core of the product. `IMTransformService` +
`EmailBodyCleaner` + `ConversationKeyService`:

- Strip quoted replies (`> ...`, "On … wrote:").
- Group consecutive same-sender messages into bubble runs
  (`BubbleRun`, `BubbleMessage`).
- Derive a `conversationKey` from the set of participants so a chain of
  emails with the same people renders as one conversation regardless
  of `Message-ID` chain quirks.

## Commands

- Build: `./mvnw clean package`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`
- Run locally: `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run
  -Dspring-boot.run.profiles=local`

## Related repositories

- **`conexus-mail-mobile`** — the ConexusMail Flutter mobile client
  that consumes `docs/API.md`.
