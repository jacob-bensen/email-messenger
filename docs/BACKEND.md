# BACKEND.md — email-messenger

Reverse-engineered from the current code on 2026-08-31. This is the
canonical description of the backend's structure so the mobile client
(`conexus-mail-mobile`) and any future contributors can rely on a stable
picture. Whenever a real change lands, update this file in the same
commit.

## Role

`email-messenger` (product name: **ConexusMail**) is a Spring Boot 3
application that plays three roles at once:

1. **Web app.** Thymeleaf-served pages under `/` — this is the marketing
   site, the account/settings UI, and the `/chats` texting-style
   workspace.
2. **REST API.** JSON endpoints under `/api/v1/**` consumed by the
   mobile client. See [`API.md`](API.md) for the canonical contract.
3. **Persistence + business logic owner.** The backend is
   **authoritative** for all state. The mobile client mirrors rules in
   its `lib/domain/` but the server always validates and recomputes
   anything security- or economy-relevant.

Both the web pages and the REST API MUST go through the same service
layer — never fork business logic between them.

## Tech stack

- **Java 21** (LTS) via `spring-boot-starter-parent:3.5.14`.
- **Spring Boot 3** starters: `web`, `thymeleaf`, `data-jpa`,
  `validation`, `mail`, `security`, `actuator`, `oauth2-client`.
- **PostgreSQL 16** in production and local dev; **H2** for tests. The
  base `application.yml` reads `JDBC_DATABASE_URL /
  JDBC_DATABASE_USERNAME / JDBC_DATABASE_PASSWORD` env vars.
- **Flyway** for schema migrations — files under
  `src/main/resources/db/migration/V{n}__*.sql`. Migrations are
  immutable once merged.
- **JPA / Hibernate** for persistence (not jOOQ — the routine assumes
  jOOQ; this project uses JPA and that is deliberate).
- **JUnit 5** + Spring Boot Test + **Testcontainers** for integration
  tests, **GreenMail** for SMTP/IMAP.
- **Maven** with wrapper (`./mvnw`).

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java         # main
├── activation/                            # activation-nudge emails to new users
├── admin/                                 # /admin/** operator surface + metrics
├── api/                                   # REST API for the mobile client (JSON)
├── auth/                                  # form + Google OIDC login, security config
├── billing/                               # Stripe checkout + webhook + plan changes
├── digest/                                # weekly digest emails
├── domain/                                # JPA entities
├── email/                                 # IMAP polling, IDLE, SMTP send, parsing
├── reengagement/                          # win-back emails for inactive users
├── repository/                            # Spring Data repositories
├── service/                               # email parsing, IM transform, threading
├── team/                                  # team + invite entities/services
└── web/                                   # Thymeleaf controllers + view models
src/main/resources/
├── templates/                             # Thymeleaf views
├── static/                                # CSS, JS, PWA assets
├── db/migration/                          # Flyway migrations (V1__init.sql … V35)
└── application.yml
```

`api/` is where the mobile-facing REST controllers live. It is a
separate package from `web/` on purpose: `web/` returns Thymeleaf views;
`api/` returns JSON.

## Authentication and sessions

- **Web browser:** Spring Security form login (`POST /login`,
  `usernameParameter=email`, `passwordParameter=password`) plus
  Google OIDC (`/auth/google/start`, activated only when
  `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` are set). Session is a
  standard `JSESSIONID` cookie. Remember-me is persisted in
  `persistent_logins`.
- **Mobile client:** currently **TBD** — the REST API surface has only
  a public `GET /api/v1/status` endpoint today. A token-based auth
  scheme (issued from a session or from a login endpoint) is a
  near-term item; see `INTERNAL_TODO.md`.
- CSRF is on for the web app; `/h2-console/**` and `/billing/webhook`
  are exempt. `/api/**` will need its own CSRF stance once mutating
  endpoints exist (planned exemption: bearer-token auth, no cookies).

## Domain model

Entities in `com.emailmessenger.domain`:

- `User` — the account holder. Keyed by unique `email`. Fields include
  `displayName`, `googleSubject`, `enabled`, timestamps, and
  `sync_frequency_minutes` (nullable — plan default when null).
- `MailAccount` — a connected mailbox (`imap_password` or
  `gmail_oauth`). Owns polling state:
  `last_seen_uid`, `last_seen_sent_uid`, encrypted credentials,
  `signature`.
- `EmailThread` — a merged conversation, keyed by `conversationKey`
  (participant set) rather than `Message-ID`, so scattered replies
  from the same set of people group into one chat.
- `Message` — one email. Fields: sender, recipients, plain + html
  bodies, `sent_at`, thread FK, outbound flag.
- `MessageRecipient` — join row with `RecipientType` (to/cc/bcc).
- `Participant` — deduped by normalized email address; `displayName`
  + avatar hash.
- `Attachment` — filename, mime, size, `blob_ref` (currently null;
  attachment bytes aren't persisted yet — roadmap item).
- `Subscription` + `Plan` (enum: FREE / PRO / BUSINESS) —
  Stripe-mirrored billing state.
- `Team` + `TeamMember` + `TeamInvite` — team workspaces.
- `SavedSearch` — named filter chips on `/chats`.
- Audit rows: `AuthEvent`, `PlanChangeEvent`,
  `EmailVerificationToken`, `PasswordResetToken`.

## Services (selected)

- `com.emailmessenger.web.ChatService` — the workspace's read model:
  builds the rail list and open-pane conversations for the logged-in
  user. This is what the mobile app's chats view will call through the
  API layer.
- `com.emailmessenger.web.ConversationListService` — merges threads
  into conversations keyed by `conversationKey`.
- `com.emailmessenger.web.OutboundMessageService` /
  `com.emailmessenger.service.ReplyService` — sends outbound replies
  via SMTP, threading onto the correct `In-Reply-To` / `References`.
- `com.emailmessenger.service.IMTransformService` /
  `EmailBodyCleaner` — strips quoted replies and collapses runs of
  same-sender messages into IM bubbles.
- `com.emailmessenger.email.MailboxPollingService` /
  `ImapIdleService` — pulls new mail (polling on Free, IDLE on paid
  tiers behind `mailbox.idle.enabled`).
- `com.emailmessenger.web.ChatEventStream` — SSE stream at
  `/chats/events` for live rail updates. The mobile analogue is TBD
  (WebSocket or long-poll — decision deferred; see
  `INTERNAL_TODO.md`).

## Build and run

- Build: `./mvnw clean package`
- Test: `./mvnw test` (single: `./mvnw test -Dtest=ClassName#method`)
- Run locally (needs local Postgres — see `db/init.sql`):
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run
   -Dspring-boot.run.profiles=local`

## Related documents

- [`API.md`](API.md) — REST contract shared with the mobile client.
- [`WEB.md`](WEB.md) — Thymeleaf page inventory.
- Repository root: `CLAUDE.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`.
