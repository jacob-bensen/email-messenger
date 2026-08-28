# BACKEND

Reference for `email-messenger`, the backend of the ConexusMail product.
It owns all persistence, business rules, mailbox integration, and — today —
also serves the public/marketing website and the signed-in web app via
Thymeleaf. A mobile client (`conexus-mail-mobile`) is planned but the JSON
REST API it will consume does not yet exist; see [API.md](API.md) and
[../INTERNAL_TODO.md](../INTERNAL_TODO.md).

## Tech

- Java 21, Spring Boot 3.x
- Spring MVC + Thymeleaf for server-rendered pages
- Spring Data JPA (Hibernate) on PostgreSQL 16 — H2 only for tests
- Flyway for schema migrations (`src/main/resources/db/migration/V*.sql`,
  35 migrations as of `V35__mail_account_signature.sql`)
- Spring Security with form login + Google OIDC
- Jakarta Mail for IMAP + SMTP
- Stripe SDK for billing
- Maven with wrapper; JUnit 5 + Testcontainers + GreenMail for tests

Note that the generic full-stack routine's stack list mentions jOOQ; this
project uses Spring Data JPA instead. Follow the existing code.

## Package layout (`com.emailmessenger`)

```
├── EmailMessengerApplication.java   Spring Boot entry point
├── web/          Thymeleaf controllers, form objects, view models
├── service/      Chat/IM transform, threading, reply, conversation-key
├── domain/       JPA entities (User, MailAccount, EmailThread, Message,
│                 Participant, Attachment, Subscription, Team, …)
├── repository/   Spring Data repositories
├── email/        IMAP polling, IMAP IDLE, SMTP, mailbox provisioning
├── auth/         Users, passwords, Google OIDC, sessions, throttling,
│                 account settings
├── billing/      Stripe checkout/portal/webhook, plan limits, upgrade
│                 nudges, trial-end conversion
├── team/         Team memberships and invitations
├── digest/       Weekly digest emails and opt-out endpoint
├── activation/   Onboarding drip for new users
├── reengagement/ Re-engagement drip for inactive users
└── admin/        Operator dashboards (revenue, churn, win-back)
```

## Key domain entities

- `User` — account owner. Has an `AuthType` (password / Google OIDC), a
  `Subscription` (Free / Pro / Business), team membership, and preferences
  (appearance, digest, notification).
- `MailAccount` — one connected mailbox per row. `AuthType` is either
  IMAP-password or Gmail-OAuth. Carries polling policy, per-mailbox
  signature, and cursors (`last_seen_uid`, `last_seen_sent_uid`) used by
  the polling and sent-folder sync loops.
- `EmailThread` — a threaded conversation as pulled from IMAP: subject,
  participants, header-chain thread id, unread flag, and a
  `conversation_key` (V32) that groups threads by the set of people
  involved so the chat UI can merge sibling threads into one bubble
  stream.
- `Message` — one RFC 822 message: sender, recipients, plain + html body,
  sent-at, header (`Message-ID`, `In-Reply-To`, `References`), and an
  `outbound` flag (V30) for messages we sent.
- `Participant` — a person, deduped by normalized email address. Display
  name + avatar hint.
- `Attachment` — filename, mime, size; `blob_ref` is currently always
  null (blob storage is a roadmap item).
- `Subscription`, `PlanChangeEvent`, `PlanLimitKind` — Stripe-backed
  plans and per-plan caps (mailbox count, retention, etc.).
- `Team`, `TeamMember`, `TeamInvite`, `TeamMemberRole` — multi-user
  workspaces on the Business plan.
- `SavedSearch` — user-defined saved filters, with per-search last-viewed
  timestamp used by the sidebar unread counts.
- `AuthEvent`, `EmailVerificationToken`, `PasswordResetToken` — auth
  audit + credentialing.

## Service layer boundaries

Controllers (both Thymeleaf and the REST endpoints once added) MUST go
through the service layer. No business logic lives in `web/*Controller`
templates; a mobile REST endpoint and a Thymeleaf page must call the same
service so behavior cannot drift between the two.

Concrete examples:

- `ConversationListService` and `ChatService` — build the chat rail and
  the per-conversation bubble stream, used by `ConversationController`.
- `ReplyService` and `OutboundMessageService` — persist an outbound
  message and hand it to SMTP. Used by the compose/reply UI.
- `MailAccountService` + `MailboxPollingService` + `ImapIdleService` —
  own connection lifecycle; scheduled poll and paid-tier IDLE both push
  through the same import pipeline.
- `IMTransformService` + `EmailBodyCleaner` — quoted-reply stripping,
  bubble runs, signature lifting.
- `PlanLimitService` — enforces per-plan caps on mailbox count, thread
  retention, team seats, etc.
- `BillingService` (+ `StripeCheckoutGateway`, `StripePortalGateway`,
  `StripeSubscriptionGateway`, `StripeWebhookGateway`) — Stripe surface,
  behind swappable interfaces so tests inject fakes.

## Auth model

Sessions are Spring Security web sessions. `SecurityConfig` wires:

- Form login at `/login` (POST), with brute-force throttling by
  `LoginThrottleFilter` + `LoginThrottleService`.
- Google OIDC via `/auth/google/start` → Spring Security OAuth2 client →
  `GoogleOidcUserService` + `OAuth2ProvisioningService`.
- Password reset via `/password/forgot` and `/password/reset` (tokens in
  `password_reset_tokens`, `V14`).
- Email verification via `/verify-email` (tokens in `email_verifications`,
  `V15`).
- Logout, remember-me, CSRF as Spring defaults.

Users authenticate against `DbUserDetailsService`. `UserActivityService`
touches `users.last_seen_at` on request so the digest and activation
drips know who is dormant.

The mobile client will need an additional token-based auth surface
(bearer/OAuth). This is unimplemented — see `INTERNAL_TODO.md`.

## Data access

Spring Data repositories live in `com.emailmessenger.repository`. Entities
are annotated JPA classes; DTOs and view models are Java records. Migrations
are strictly append-only: never edit an already-shipped `V{n}__*.sql`, add a
new one. See `V33__drop_thread_notes.sql` as an example of retiring a
feature via a new migration rather than editing the old one.

## Scheduled work

- `MailboxPollingService` — periodic IMAP poll for INBOX and Sent folder
  (cursors `last_seen_uid`, `last_seen_sent_uid`).
- `ImapIdleService` — long-lived IMAP IDLE for paid-tier accounts, behind
  the `mailbox.idle.enabled` flag.
- `ChatEventStream` (SSE at `/chats/events`) — receives `NewMailEvent`s
  from the polling/IDLE services and pushes to open browser tabs.
- `TrialEndConversionScheduler`, activation and reengagement drips —
  cron-driven; each behind an `*_ENABLED` env flag. See
  `master/MASTER_ACTIONS.md` for the flags that still need to be flipped
  on the deploy.

## PWA

`PwaController` serves the manifest, icons, service worker (`/sw.js`), and
`/offline`. The client is currently browser-only; the mobile app is a
separate Flutter repo (`conexus-mail-mobile`) that will consume the JSON
API once it exists.

## Testing

- `./mvnw test` — full suite; H2 via `src/test/resources/application.yml`.
- Integration tests use Testcontainers (Postgres) and GreenMail (SMTP +
  IMAP) so mailbox flows can be exercised end-to-end.
- Single test: `./mvnw test -Dtest=ClassName#method`.

## Running locally

1. Bootstrap the DB once: `psql -U postgres -f src/main/resources/db/init.sql`
   (creates the `email_messenger` database + `email_messenger` app user).
2. `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
3. App listens on `http://localhost:8080`.
