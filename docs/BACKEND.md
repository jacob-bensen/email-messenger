# BACKEND

The Spring Boot application that powers **ConexusMail / email-messenger**:
it owns the marketing site, the authenticated `/chats` workspace, all
persistence, and every piece of authoritative business logic. It is the
counterpart of the `conexus-mail-mobile` client.

## Stack

- Java 21 · Spring Boot 3.x
- PostgreSQL 16 in prod & local dev, H2 in tests (see
  `src/test/resources/application.yml`)
- Flyway for schema migrations (`src/main/resources/db/migration/`)
- Thymeleaf for server-rendered pages
- Jakarta Mail for IMAP / SMTP
- Maven (via `./mvnw`)

> NOTE — jOOQ. The routine's target stack lists jOOQ. This project uses
> Spring Data JPA repositories, not jOOQ. Documented here so a sync
> pass doesn't try to reconcile a non-existent codegen step.

## Runtime layout

Package root: `com.emailmessenger`.

- `EmailMessengerApplication.java` — Spring Boot entry point.
- `web/` — Thymeleaf controllers, form objects, chat SSE stream. See
  `docs/WEB.md` for the full page inventory.
- `service/` — chat conversation building, IM transform, reply
  composition, participant / conversation-key derivation.
- `email/` — IMAP polling + IDLE, Gmail OAuth, mailbox credential
  encryption, RFC-822 parsing, outbound send.
- `domain/` — JPA entities (see "Domain model" below).
- `repository/` — Spring Data JPA interfaces.
- `auth/` — form login, Google OIDC, email verification, password reset,
  account settings.
- `billing/` — Stripe checkout, portal, cancel, webhook.
- `admin/` — operator dashboard at `/admin/revenue`, retention queue,
  win-back outreach, reconciliation.
- `digest/`, `reengagement/`, `activation/` — scheduled mail flows
  (weekly digest, at-risk drips, activation drip). All gated behind
  `*.enabled` env flags.
- `team/` — team invites, membership, roles.

## Domain model

Entities live in `com.emailmessenger.domain`. Highlights:

- **User** — account, plan, verification state, mail preferences.
- **Subscription** — tie to Stripe subscription; carries plan tier,
  status, cancellation reason, win-back stamps.
- **Plan** — pricing tier enum with per-plan limits (mailbox count,
  thread cap, saved-search cap).
- **MailAccount** — a connected IMAP/OAuth mailbox owned by a User.
  Stores encrypted IMAP credentials (`CredentialEncryptor`), Gmail OAuth
  refresh tokens, last-seen UIDs for inbox + sent folder, polling
  policy, signature.
- **EmailThread** — grouped messages, keyed by RFC threading headers.
  Since V32 also carries a stable `conversationKey` (the
  participant-set hash the `/chats` list groups on).
- **Message** — one email row: sender, subject, plain/html body,
  timestamps, IMAP UID, outbound flag.
- **MessageRecipient** — normalized To / Cc / Bcc rows per message.
- **Participant** — deduped by normalized email address; display name.
- **Attachment** — filename, mime, size, `blob_ref` (currently always
  null — see `docs/API.md` "Known gaps").
- **SavedSearch**, **Team**, **TeamInvite**, **TeamMember** —
  self-explanatory.
- **AuthEvent**, **PlanChangeEvent**, **CancellationReason** — audit /
  telemetry records feeding the admin dashboard.
- **EmailVerificationToken**, **PasswordResetToken** — single-use
  credential tokens.

Migrations to date: `V1__init.sql` through `V35__mail_account_signature.sql`.
Flyway migrations are immutable once merged — always add a new
`V{n}__*.sql` rather than editing an existing file.

## Auth model

- Form login and Google OIDC (`auth.google.client-id/secret`), backed by
  Spring Security. Google is activated only when both env vars are set;
  otherwise the button hides and the OAuth filter chain isn't wired.
- Session cookie based; no bearer/JWT layer today. **A mobile client
  will need one** — see `docs/API.md` "Known gaps".
- `/admin/**` is gated by an operator allowlist (`admin.emails`,
  comma-separated). Non-admins get a 404.
- Mail-account credentials (IMAP passwords, Gmail refresh tokens) are
  encrypted with `mailbox.encryption.password` + `mailbox.encryption.salt`
  before persisting.

## Mail pipeline

1. `MailboxPollingScheduler` fires `MailboxPollingService.poll(...)` per
   connected mailbox on a `@Scheduled` cadence (`mailbox.polling.enabled`).
2. For paid-tier mailboxes, `ImapIdleService` also holds an IDLE
   connection (`mailbox.idle.enabled`) for seconds-latency delivery.
3. New messages are parsed, threaded, and persisted; `NewMailEvent` is
   published.
4. `ChatEventStream` fans events out to the `/chats/events` SSE endpoint
   so open browser tabs update in place.
5. Outbound sends go through `OutboundMessageService` +
   `JavaMailSender`; the Sent folder is later re-scanned
   (`last_seen_sent_uid`) so the outbound bubble threads back in.

## Configuration

Base config in `application.yml`; local dev overrides in
`application-local.yml` (`--spring.profiles.active=local`). Every secret
comes from env vars — see the head of `application.yml` and the
"Standing constraints / OAuth & third-party APIs / Stripe / Mailbox
encryption" sections of `master/MASTER_ACTIONS.md` for the full list of
what a prod deploy needs.

## Commands

- Build: `./mvnw clean package`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`
- Run locally (needs local Postgres, see `db/init.sql`):
  `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

There is intentionally no Dockerfile and no CI workflow — see
`master/PLAN.md` "Standing constraints" (owner decision 2026-06-11).

## Planning docs (repo-specific, distinct from `docs/`)

`master/` holds the working-agreement planning system used by autonomous
routines:

- `PLAN.md` — current Primary Objective + milestones.
- `BACKLOG.md` — up to 10 next items feeding PLAN milestones.
- `MASTER_ACTIONS.md` — items genuinely blocked on a human
  (credentials, hosting, legal, assets).
- `CHANGELOG.md` — dated log of shipped work.

`docs/` is different: it's the contract surface shared with the mobile
client. Keep them accurate on every cross-repo run.

## Related repositories

- **`conexus-mail-mobile`** — the Flutter mobile client. Currently a
  stub (README + `.gitignore` only); no Flutter scaffold yet. Its
  `docs/DESIGN.md` is the product / app design source of truth; its
  `docs/MOBILE.md` mirrors this document. The shared API contract lives
  in **`docs/API.md`** in *this* repo.
