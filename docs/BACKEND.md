# BACKEND — email-messenger (ConexusMail)

This is the Spring Boot 3 application that owns:

- The **public website** (Thymeleaf-rendered pages: landing, pricing, legal,
  auth flows, mailbox connect, chats workspace, admin).
- All **authoritative business logic** — user accounts, subscriptions (Stripe),
  mailbox connections (IMAP password + Gmail OAuth), email import/threading,
  the IM transform, teams, saved searches, digests, admin metrics.
- All **persistence** — PostgreSQL 16 in prod/local, H2 for tests. Flyway
  migrations under `src/main/resources/db/migration/` are the single source
  of truth for schema.
- The (currently thin) **REST/JSON API** used by non-browser clients such
  as the future ConexusMail mobile app (see `docs/API.md`). Today only
  `POST /chats/refresh` returns JSON; everything else is Thymeleaf.

## Runtime

- Java 21, Spring Boot 3.x. Maven wrapper: `./mvnw`.
- Local run: `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
  (bootstrap Postgres with `db/init.sql`).
- Tests: `./mvnw test` (H2 via `src/test/resources/application.yml`).

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/     # welcome-back / re-engagement scheduling
├── admin/          # /admin/* metrics (revenue, churn, funnel, win-back)
├── auth/           # user accounts, sessions, Google OIDC, password reset
├── billing/        # Stripe checkout, webhooks, plan limits, banners
├── digest/         # scheduled email digests
├── domain/         # JPA entities (User, EmailThread, Message, Participant, …)
├── email/          # IMAP polling, IMAP IDLE, Gmail OAuth mailbox, SMTP send
├── reengagement/   # dormant-user outreach
├── repository/     # Spring Data repositories
├── service/        # IM transform, conversation keying, body cleaning, reply
├── team/           # team invites & membership
└── web/            # Thymeleaf controllers, form objects, chat SSE stream
```

Templates live under `src/main/resources/templates/` (see `docs/WEB.md`
for the page inventory). Static CSS/JS under `src/main/resources/static/`.

## Auth model

Form login (username = email) plus Google OIDC. Sessions are Spring
Security's default `HttpSession`-backed. `SecurityConfig` in `auth/`
gates all `/chats/**`, `/mailboxes/**`, `/account/**`, `/admin/**`
behind an authenticated principal; `/`, `/pricing`, `/legal/*`, and the
sign-in / register routes are open.

There is **no token-based auth today**. The mobile client will need
either a cookie-based session (same login endpoint) or a bearer-token
scheme; that decision is deferred to `docs/API.md`.

## Persistence

- All entities are JPA (`@Entity` classes, not records). DTOs and view
  models are records.
- Migrations are immutable once merged — add a new `V{n}__*.sql` rather
  than editing an existing one. Highest applied version at the time of
  writing this doc: **V35** (`mail_account_signature.sql`).
- Repositories under `com.emailmessenger.repository` are Spring Data JPA.

## Key domain concepts

- **`User`** — the account. Owns `MailAccount`s, `Subscription`,
  `SavedSearch`s, `Team` memberships.
- **`MailAccount`** — one connected mailbox (IMAP password or Gmail OAuth).
  Carries polling cursors (last-seen UID, last-seen sent UID) and the
  editable signature block.
- **`EmailThread`** — a group of messages joined by RFC 5322
  `Message-ID` / `In-Reply-To` / `References` headers.
- **`Message`** — one email: sender, recipient list, plain + HTML body,
  `sent_at`, attachments, direction (inbound vs. outbound-recorded).
- **`Participant`** — a person, keyed by normalized email address.
- **`ConversationKey`** — a stable hash of the participant set (see
  `service.ConversationKeyService`). Multiple `EmailThread`s with the same
  participants roll up into ONE "chat" in the workspace, matching how
  people actually think about text-message threads.
- **`ChatConversation`** — the view model for one open chat: members,
  bubble runs (consecutive same-sender messages grouped), day separators,
  saved-signature preview.
- **`Subscription` / `Plan`** — Free / Pro / Business, Stripe-backed.
  Plan limits (mailbox count, saved searches, thread history) are
  enforced through `PlanLimitService` — every user-facing mutation that
  can hit a cap goes through it.

## Live delivery

- **IMAP IDLE** for paid-tier accounts (`ImapIdleService`, feature-flagged
  by `mailbox.idle.enabled`).
- **SSE stream** at `GET /chats/events` (`ChatEventStream`) publishes
  `NewMailEvent`s from `MailboxPollingService` to the open chat page so
  the rail and open pane refresh in place.
- Foreground heartbeat at `POST /chats/refresh` triggers a synchronous
  refresh and returns `{ imported: N }`.

## Website ↔ API sharing

**Golden rule:** Thymeleaf controllers and the JSON API MUST share the
same service layer. Never fork business logic into a controller or a
template — anything the server considers authoritative lives under
`service/`, `email/`, `billing/`, `auth/`, `admin/` and is called from
both surfaces. The current codebase upholds this: controllers are thin
and delegate; the JSON `refresh` endpoint calls
`MailboxPollingService.refreshActiveUserNow` — the same service the
scheduled poller uses.

## Testing

- JUnit 5 + Spring Boot Test.
- Slice tests where possible (`@WebMvcTest`, `@DataJpaTest`).
- Integration tests use **Testcontainers** (Postgres) and **GreenMail**
  (SMTP/IMAP) so the mail path is exercised end-to-end without a live
  server.

## Conventions

- Constructor injection only; no field `@Autowired`.
- Package-private by default; widen visibility only when needed.
- Records for DTOs & view models; classes for entities.
- Don't add comments that just restate the code — only non-obvious *why*.
- No backwards-compatibility shims while pre-1.0.

## Related repositories

- **conexus-mail-mobile** — the Flutter mobile client that will consume
  the API defined in `docs/API.md`. It mirrors server rules in its
  `lib/domain/` package; the server is authoritative for all state.

## Doc set

- `docs/BACKEND.md` — this file.
- `docs/WEB.md` — Thymeleaf page inventory, routes, service usage.
- `docs/API.md` — REST/JSON contract (single source of truth for
  backend ↔ mobile).
- `CLAUDE.md` — repo conventions and roadmap.
- `CHANGELOG.md` — dated log of routine runs.
- `INTERNAL_TODO.md` — known gaps and deferred work.
