# Backend architecture

Reverse-engineered from the code on 2026-11-11. Product name: **ConexusMail**.
Anything not verifiable from the source is marked **TBD**.

## Stack (actual)

- Java 21, Spring Boot 3.5.x, Maven (wrapper checked in).
- Persistence: **Spring Data JPA / Hibernate** (`ddl-auto: validate`,
  `open-in-view: false`) over PostgreSQL 16; H2 in tests. **Not jOOQ** — there
  is no codegen step.
- Flyway migrations `V1`–`V35` in `src/main/resources/db/migration`.
- Spring Security (form login + optional Google OIDC), Thymeleaf, Jakarta
  Mail (IMAP/SMTP), jsoup (HTML sanitising), stripe-java, Actuator (health only).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers for the chats workspace, mailboxes, saved searches, landing/pricing/legal/SEO/PWA; `ChatEventStream` (SSE); `ChatService`, `ConversationListService`, `OutboundMessageService`; model advice; `GlobalExceptionHandler`. |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), view-model records (`ChatConversation`, `ChatMember`, `SenderBubbleRun`, `BubbleMessage`, `ConversationListItem`, `ThreadRef`). |
| `email` | Mailbox integration: `MailAccountService`, `MailboxPollingService` (+ scheduler), `ImapIdleService` (live delivery, paid tiers), `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor`, `OwnerAddressService`, `NewMailEvent`. |
| `auth` | `SecurityConfig`, registration/login, Google OIDC provisioning, email verification, password reset, login throttling + audit, account settings (`AccountService`). |
| `billing` | Stripe checkout/portal/webhook, `BillingService`, plan caps (`PlanLimits`, `PlanLimitService`), trial nudges, billing banner. |
| `team` | Teams and invites. |
| `admin` | Operator dashboard `/admin/revenue` and its metrics services, weekly operator digest, win-back outreach. |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails, each behind an `*.enabled` flag. |
| `domain` | JPA entities and enums. |
| `repository` | Spring Data repositories. |

## Core concepts

- **Conversation key.** A chat is the set of *people* involved, not an email
  thread. `ConversationKeyService` hashes the sorted, normalised non-owner
  participant addresses (SHA-256, first 32 hex chars) into
  `email_threads.conversation_key`. All threads sharing a key merge into one
  timeline. `/chats/{key}` uses this key.
- **Mailbox scope.** With more than one connected account, a view can be
  scoped to one account's address (`?mailbox=`); with a single account
  nothing is scoped. No scope = the cross-account "Hub" dashboard.
- **Import.** `MailboxPollingService` polls IMAP (inbox + Sent folder, UID
  cursors per account), `EmailImportService` threads by
  `Message-ID`/`In-Reply-To`/`References`, and publishes `NewMailEvent`.
  `ImapIdleService` holds IDLE connections for paid-tier accounts.
- **Send.** `ReplyService` sends over SMTP (`JavaMailSender`);
  `OutboundMessageService` records the outbound message locally.
- **Plans.** `Plan` = `FREE | PRO | BUSINESS`. Caps (`PlanLimits`): Free 3
  mailboxes / 500 threads; Pro 5 mailboxes / unlimited threads; Business
  unlimited. Saved searches are unlimited on every plan.
  `PlanLimitExceededException` is turned into an upgrade-modal redirect.

## Schema overview

Tables (see migrations for columns): `users`, `persistent_logins`
(remember-me), `email_verifications`, `password_reset_tokens`, `auth_events`,
`subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads`,
`messages`, `message_recipients`, `participants`, `attachments`,
`saved_searches`, `digest_email_preferences`, `teams`, `team_members`,
`team_invites`. (`thread_notes` was created in V25 and dropped in V33.)

`attachments.blob_ref` is always null today — attachment bytes are not stored.

## Auth model

- Session cookie (`JSESSIONID`) via form login at `POST /login`
  (`email` + `password`), optional persistent remember-me cookie (30 days),
  optional Google OIDC login when `GOOGLE_CLIENT_ID`/`_SECRET` are set.
- CSRF is enabled for everything except `/h2-console/**` and
  `/billing/webhook`.
- Public routes are allow-listed in `SecurityConfig`; everything else
  requires an authenticated session. `/admin/**` additionally requires the
  operator allowlist (`admin.emails`) and 404s otherwise.
- Login throttling (`LoginThrottleFilter`) runs before credential checks.
- **There is no token/bearer authentication.** A mobile client cannot
  authenticate today except by driving the browser form flow.

## Web pages vs. REST API

Today the app is **server-rendered only**. There is no `/api/**` namespace
and no `@RestController` other than the Stripe webhook. The only JSON/SSE
endpoints are the ones `fragments/live-refresh.html` uses (see `API.md`).

Rule going forward: REST controllers and Thymeleaf controllers must call the
same services. Known places where business logic currently lives in a
*controller* and would be duplicated by an API are listed in
`INTERNAL_TODO.md` (chiefly the send path in `ConversationController`).

## Configuration

Environment variables read by `application.yml`: `JDBC_DATABASE_URL`,
`JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`,
`MAIL_USER`, `MAIL_PASS`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`,
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `MAILBOX_ENCRYPTION_PASSWORD`,
`MAILBOX_ENCRYPTION_SALT`, `MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`.
Further property classes (`BillingProperties`, `SiteProperties`,
`LandingProperties`, `LegalProperties`, `AdminProperties`) bind additional
keys — see each class.
