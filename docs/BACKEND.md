# ConexusMail backend architecture

Spring Boot **3.5** on Java **21**, Maven wrapper, one deployable jar. It
serves the website (Thymeleaf, `docs/WEB.md`), will serve the mobile REST API
(`docs/API.md`), and owns all persistence and business rules.

Note for the sync routine: persistence is **Spring Data JPA / Hibernate**,
not jOOQ. There is no codegen step. Hibernate runs with `ddl-auto: validate`
against the Flyway schema, and `open-in-view` is off, so anything a view or
API response needs must be loaded inside the service transaction.

## Packages (`com.emailmessenger`)
| Package | Responsibility |
|---|---|
| `domain` | JPA entities and enums (`Plan`, `AuthType`, `RecipientType`, …) |
| `repository` | Spring Data repositories; every user-data query is scoped by owner |
| `service` | Conversation model: `ConversationKeyService`, `ConversationService` (bubble runs), `ReplyService` (SMTP send), `EmailBodyCleaner` / `IMTransformService` (quote + signature stripping, markdown), view-model records |
| `email` | Mailbox integration: `MailAccountService` (connect/list/remove), `EmailImportService` (MIME → threads), `MailboxPollingService` + `PollingPolicy`, `ImapIdleService`, `JakartaImapClient`, `GmailOAuthClient`, `CredentialEncryptor` |
| `web` | Thymeleaf controllers plus the chat-facing services `ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `ChatEventStream` (SSE) |
| `auth` | `SecurityConfig`, users, registration, Google OIDC, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` / `PlanLimitService`, trial and banner logic |
| `team` | Teams and invites (membership only — teams grant no data access yet) |
| `digest`, `activation`, `reengagement` | Lifecycle email jobs |
| `admin` | Operator revenue/churn dashboards and win-back outreach |

## Core concepts
- **Thread** (`EmailThread`): messages linked by `Message-ID` /
  `In-Reply-To` / `References`, per owner.
- **Conversation**: all of an owner's threads sharing a `conversation_key` —
  a hash of the set of non-owner participants (exact algorithm in
  `docs/API.md` §4). The key is recomputed on every import and reply. It is
  not a capability: every lookup pairs it with the owner.
- **Unread** is per thread, not per message. Opening a conversation marks
  its threads read.
- **Import** (`EmailImportService`): idempotent per owner on Message-ID;
  enforces the thread cap for new threads; inbound mail sets `unread`.
- **Polling** (`MailboxPollingService.pollOne`): fetches INBOX and the Sent
  folder past stored UID cursors, imports, schedules the next poll, and
  publishes `NewMailEvent(userId, importedCount)`, which `ChatEventStream`
  forwards over SSE after commit. Cadence: Free 15 min, paid 5 min (users can
  only slow it down); five consecutive failures suspend the mailbox. Paid
  mailboxes also get an IMAP IDLE worker thread.
- **Sending** (`ReplyService` then `OutboundMessageService`): mail goes out
  through the single app-wide SMTP relay with From set to the user's mailbox
  address, plain text only; a local outbound copy is then recorded. The two
  steps are not atomic.

## Database (Flyway V1–V35, PostgreSQL 16; H2 in tests)
Ids are identity `BIGINT`; timestamps are zone-less `TIMESTAMP` holding UTC.
Migrations are immutable — add `V36__…` next.

| Table | Entity | Notes |
|---|---|---|
| `users` | `User` | unique `email`; `password_hash`, `google_subject`, `email_verified_at`, lifecycle-email stamps, `sync_frequency_minutes`, `default_mail_account_id` |
| `persistent_logins` | — | Spring Security remember-me |
| `auth_events` | `AuthEvent` | login/security audit; drives the login throttle |
| `password_reset_tokens`, `email_verifications` | `PasswordResetToken`, `EmailVerificationToken` | SHA-256 token hashes with expiry |
| `mail_accounts` | `MailAccount` | per user; encrypted IMAP password or Gmail refresh token; UID cursors, failure counter, `next_poll_at`, `auth_type`, `signature`; unique `(user_id, host, username)` |
| `email_threads` | `EmailThread` | `owner_id`, `subject`, `root_message_id`, `unread`, `conversation_key`; unique `(owner_id, root_message_id)` |
| `messages` | `Message` | `thread_id`, `sender_id`, `message_id_header`, `in_reply_to`, bodies, `sent_at`, `outbound` |
| `message_recipients` | `MessageRecipient` | TO / CC / BCC |
| `attachments` | `Attachment` | metadata only; `blob_ref` is always null (bytes are not stored) |
| `participants` | `Participant` | unique `email`, shared across all users |
| `saved_searches` | `SavedSearch` | per owner, unique name |
| `subscriptions` | `Subscription` | one per user; plan, raw Stripe status, Stripe ids, period, cancellation and win-back stamps |
| `plan_change_events` | `PlanChangeEvent` | plan transitions for admin metrics |
| `teams`, `team_members`, `team_invites` | `Team`, `TeamMember`, `TeamInvite` | one owned team per user |
| `digest_email_preferences` | `DigestEmailPreference` | opt-out token and flag |

## Auth model
Browser sessions only (details in `docs/API.md` §2): form login with BCrypt,
optional Google OIDC (enabled when the Google client id/secret are set),
persistent remember-me, CSRF on everything except the Stripe webhook. There
are no roles; admin is an email allowlist checked in `AdminAuthorizer`.
Controllers resolve the user with
`UserService.requireByEmail(principal.getName())` and pass the `User` into
services, which scope every query by it. Not-owned resources surface as 404.

Two methods take a bare id and rely on the caller to check ownership first:
`MailboxPollingService.pollOne(accountId)` and
`ReplyService.sendReply(threadId, …)`. A REST controller must do the same
owner-scoped lookup the web controllers do.

## Transactions
Method-level `@Transactional` only. Services that return view models
(`ChatService`, `ConversationListService`) build them fully inside a
read-only transaction. `MailboxPollingService.pollOne` uses a
`TransactionTemplate` because it is called from loops in the same class,
where an annotation would be bypassed.

## Scheduled jobs
| Job | Schedule | Flag (default) |
|---|---|---|
| Mailbox poll | every 5 min | `mailbox.polling.enabled` (on) |
| IMAP IDLE reconcile | every 60 s | `mailbox.idle.enabled` (on) |
| SSE keepalive | every 25 s | always |
| Weekly digest, re-engagement, activation ×3, trial-end, operator digest | cron, UTC | `digest.enabled`, `reengagement.enabled`, `activation.enabled`, `trial-end.enabled`, `admin.weekly-digest.enabled` (all off) |

## Configuration
`application.yml` reads `JDBC_DATABASE_URL` / `_USERNAME` / `_PASSWORD`,
`MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASS`, `GOOGLE_CLIENT_ID` /
`GOOGLE_CLIENT_SECRET`, `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET`,
`MAILBOX_ENCRYPTION_PASSWORD` / `MAILBOX_ENCRYPTION_SALT`. The only Spring
profile is `local`. Stripe and Google features are inert when their keys are
blank.

## Tests
`./mvnw test` — about 800 tests, mostly `@SpringBootTest` on H2 with the real
Flyway migrations and mocked seams (`ImapClient`, `JavaMailSender`, Stripe
gateways). One Testcontainers + GreenMail end-to-end test runs only when
Docker is available.
