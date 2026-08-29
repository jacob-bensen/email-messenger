# BACKEND — email-messenger (ConexusMail server)

Spring Boot 3 / Java 21 monolith. Serves the entire product today:
the Thymeleaf website, the authenticated `/chats` workspace, mailbox
imports, billing, admin metrics, and the SSE live-update stream.

There is **no JSON REST API for a mobile client yet**. Every controller
listed in `WEB.md` returns Thymeleaf HTML, an HTMX fragment, a redirect,
or (for `/chats/events`) a Server-Sent Events stream. Building a
JSON API surface is a prerequisite for any mobile client work — see
`INTERNAL_TODO.md`.

## Module map (`com.emailmessenger`)

- `EmailMessengerApplication` — Spring Boot entry point.
- `web/` — Thymeleaf controllers, form/view records, SSE emitter
  (`ChatEventStream`), PWA manifest and service-worker endpoints,
  `NavModelAdvice` / `SiteModelAdvice` (global model attributes).
- `auth/` — form + Google OIDC login, registration, email verification,
  password reset, account settings, `SecurityConfig`, throttling filter,
  Google OAuth client wiring. `UserService`, `AccountService` are the
  service seams the web layer uses.
- `billing/` — Stripe checkout, portal, and webhook gateways;
  `PlanLimitService` enforces per-plan caps (`PlanLimitKind`);
  `BillingBannerService`, `TrialConversionNudgeService`,
  `TrialEndConversionScheduler`.
- `email/` — mailbox integration: `MailAccountService` (CRUD, credentials
  encrypted by `CredentialEncryptor`), `ImapClient`/`JakartaImapClient`,
  `MailboxPollingScheduler` + `MailboxPollingService` (periodic import),
  `ImapIdleService` + `JakartaImapIdleClient` (paid-tier IDLE),
  `EmailImportService` (RFC 822 parsing, threading, sent-folder sync),
  `MimeMessageParser`, `GmailOAuthClient` (refresh tokens),
  `OwnerAddressService`, `NewMailEvent`.
- `service/` — the IM-conversion domain: `EmailBodyCleaner` (strip quotes
  and signatures), `IMTransformService` (collapse into bubble runs),
  `ConversationService`, `ConversationListService`, `ChatService`,
  `ConversationKeyService` (participant-set key that groups threads into
  one chat), `ReplyService` (outbound reply through SMTP + threading
  headers), `OutboundMessageService`, `DemoConversationService`.
- `domain/` — JPA entities (see schema section).
- `repository/` — Spring Data JPA repositories, one per aggregate.
- `admin/` — internal metrics: revenue, churn, funnel, at-risk retention,
  onboarding funnel, win-back, trial-end conversion,
  `AdminWeeklyDigestScheduler`; access gated by `AdminAuthorizer`.
- `team/` — Team / TeamInvite services + invite acceptance controller.
- `digest/` — weekly per-user digest email (`WeeklyDigestService`) plus
  opt-out endpoint.
- `activation/`, `reengagement/` — lifecycle email schedulers.

## Persistence

- PostgreSQL 16 in prod and local; H2 in tests.
- Flyway migrations in `src/main/resources/db/migration/`
  (`V1__init.sql` … currently through `V35__mail_account_signature.sql`).
  Never edit a shipped migration — add a new `V{n}__*.sql`.
- Spring Data JPA. No jOOQ. (The generic sync routine assumes jOOQ; this
  project uses JPA.)
- Local DB bootstrap: `db/init.sql`.

### Core schema (V1)

`participants` (deduped by email), `email_threads`,
`messages` (FK to thread + sender participant),
`message_recipients` (TO/CC/BCC), `attachments` (`blob_ref` unused
today — attachments are metadata-only).

### Additions layered on top

`users` (V2), `email_thread_owner` (V3), `subscriptions` (V4),
`mail_accounts` + polling cursors and policy (V5-V7),
acquisition source (V8), thread unread flag (V9),
`saved_searches` + last-viewed (V10-V11), digest opt-in (V12),
re-engagement (V13), password-reset tokens (V14),
email-verification tokens (V15), `auth_events` (V16),
subscription billing period (V17), Google subject on user (V18),
password-set flag (V19), activation nudge tracking (V20-V22),
trial-end email tracking (V23), teams and invites (V24),
plan-change events (V26), cancellation reason (V27),
win-back email tracking (V28), mail-account auth type (V29),
outbound Message-ID column (V30), last-seen sent UID cursor (V31),
`email_threads.conversation_key` (V32), thread notes removed (V33),
user mail preferences (V34), mail-account signature (V35).

## Auth & session

- Spring Security session cookies.
- Form login (`/login`) and Google OIDC via `/auth/google/start` →
  `GoogleOidcUserService` provisions users through
  `OAuth2ProvisioningService`.
- Login throttling by IP + username via `LoginThrottleFilter`.
- Password reset tokens (`PasswordResetToken`), email verification tokens
  (`EmailVerificationToken`).
- No API token / OAuth2 bearer flow exists — see `INTERNAL_TODO.md`.

## Mailbox lifecycle

1. User connects a mailbox (`/mailboxes/new` — IMAP+password, or
   `/mailboxes/gmail/connect` — Gmail OAuth).
2. Credentials encrypted by `CredentialEncryptor` before persist.
3. `MailboxPollingScheduler` polls per policy (`PollingPolicy`);
   `EmailImportService` parses RFC 822, threads by
   `Message-ID` / `In-Reply-To` / `References`, and writes to
   `messages` / `message_recipients` / `attachments`.
4. Paid-tier accounts additionally use `ImapIdleService` for push
   updates behind `mailbox.idle.enabled`.
5. Any new mail publishes `NewMailEvent`; `ChatEventStream`
   fans it out over SSE on `/chats/events` so the open browser tab
   morphs the rail and the current pane without a reload.

## The IM transform pipeline

`EmailBodyCleaner` strips `>` quotes, "On …, wrote:" citations, and
signature blocks (structured fields are surfaced separately).
`IMTransformService` sorts messages, groups consecutive same-sender
messages into `BubbleRun`s, and builds a `ChatConversation` view keyed by
`conversationKey` (a normalized participant set — one chat may aggregate
multiple RFC threads with the same people).

## Reply / send

`ReplyService` composes a MIME reply that continues the thread by
including `In-Reply-To` and `References` from the thread's last message.
Currently it always threads onto the last message, not the specific
bubble the user replied to — see the roadmap in root CLAUDE.md.

## Config

- Base: `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`.
- Local: `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`.
- Feature flags include `mailbox.idle.enabled`.

## Tests

JUnit 5 + Spring Boot Test. Testcontainers for Postgres, GreenMail for
IMAP/SMTP. `./mvnw test` from repo root.
