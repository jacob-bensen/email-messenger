# Backend architecture

The `email-messenger` repo is a Spring Boot 3 monolith that serves BOTH:

1. The **ConexusMail** website — server-rendered Thymeleaf pages for the
   marketing surface, auth, billing, and the texting-style `/chats` workspace.
2. (Planned) A **REST/JSON API** consumed by the mobile client
   (`conexus-mail-mobile`). See [`API.md`](./API.md); the API is currently
   TBD — most endpoints today return HTML.

Both must go through the SAME service layer; controllers (HTML + JSON) are
thin.

## Tech stack

- Java 21 (LTS)
- Spring Boot 3 (`spring-boot-starter-web`, `-thymeleaf`, `-data-jpa`,
  `-validation`, `-mail`, `-security`)
- PostgreSQL 16 in prod/local, H2 for tests
- Flyway for schema migrations (`src/main/resources/db/migration/`)
- Maven (wrapper) — `./mvnw`
- JUnit 5 + Spring Boot Test, Testcontainers, GreenMail

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   entry point
├── admin/                            admin metrics + retention ops
├── auth/                             form + Google OIDC + password reset
├── billing/                          Stripe checkout, portal, webhooks, plans
├── digest/                           digest email preferences + opt-out
├── domain/                           JPA entities
├── email/                            IMAP polling + IDLE, Gmail OAuth,
│                                     MIME parsing, mailbox lifecycle
├── reengagement/                     re-engagement email nudges
├── repository/                       Spring Data repositories
├── service/                          conversation, IM transform, replies
├── team/                             teams, invites
├── web/                              Thymeleaf controllers + live SSE
└── activation/                       activation nudges + follow-ups
```

## Domain model (current)

Entities under `com.emailmessenger.domain`:

- **User** — account, plan, auth state, activation tracking.
- **MailAccount** — a connected mailbox (IMAP-password OR Gmail-OAuth),
  encrypted credentials, polling cursor, signature.
- **EmailThread** — messages grouped by RFC-822 `Message-ID` /
  `In-Reply-To` / `References`. Also carries `conversationKey` — a stable
  hash of the deduped participant set for the `/chats` view.
- **Message** — one email: sender, recipients, plain + html body, sent-at,
  `Message-ID`, direction (inbound/outbound).
- **MessageRecipient** — per-message recipient rows (TO/CC/BCC).
- **Participant** — deduped by normalized email address.
- **Attachment** — filename, mime, size, `blob_ref` (currently always
  null — attachments aren't yet downloadable).
- **Subscription** — Stripe billing state (Plan, period, cancellation).
- **Team**, **TeamMember**, **TeamInvite** — team workspaces.
- **SavedSearch** — user-defined chat filters.
- **AuthEvent**, **PlanChangeEvent**, **EmailVerificationToken**,
  **PasswordResetToken** — audit + lifecycle.

Non-obvious invariants:

- `conversationKey` groups threads by their unique participant *set* so all
  Ada-plus-me threads collapse into one chat regardless of `Message-ID`
  drift. Backfilled by `ConversationKeyBackfill`.
- Sent-folder sync uses a per-account `last_seen_sent_uid` cursor
  (Flyway V31) to reconcile outbound Message-IDs with delivered mail.

## Auth model

- Form login (`/login`, `/register`) + Google OIDC (`/auth/google/start`).
- Email verification (`/verify-email`) — required before some actions.
- Session-based (`Principal`), cookie carried by browser.
- API auth for the mobile client is **TBD** — see [`API.md`](./API.md).

## Live delivery

- `MailboxPollingService` (`@Scheduled`) polls all connected mailboxes;
  paid tiers additionally use `ImapIdleService` for IMAP IDLE where the
  server supports it.
- On new mail, `NewMailEvent` is published; `ChatEventStream` forwards it
  to per-user SSE subscribers on `GET /chats/events`. The `/chats` page
  reacts in place (`fragments/live-refresh.html`); no reload.
- Not yet verified against a real IDLE-supporting IMAP server.

## Website vs. API — service reuse rule

Whenever a mobile-facing JSON endpoint is added, it MUST call the same
service the Thymeleaf controller calls. No forked business logic in the
web layer, no logic in controllers. Contract lives in [`API.md`](./API.md).

## Commands

- Build: `./mvnw clean package`
- Run local: `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`

Local dev needs Postgres 16 seeded with `db/init.sql`.
