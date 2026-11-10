# Backend architecture

Reverse-engineered from the code on 2026-11-10. Where something was not
verified by reading the source it is marked **TBD**.

## Stack (actual)

- Java 21, Spring Boot 3 (web MVC, Thymeleaf, Security, OAuth2 client,
  validation, mail, actuator).
- **Persistence is Spring Data JPA / Hibernate**, not jOOQ.
  `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`. There is no
  codegen step.
- PostgreSQL 16 in production/local; H2 in tests. Flyway migrations
  `V1`–`V35` in `src/main/resources/db/migration`.
- Jakarta Mail (IMAP + SMTP), jsoup (HTML sanitising), stripe-java.
- Tests: JUnit 5, Spring Boot Test, spring-security-test, GreenMail,
  Testcontainers (the one Testcontainers test skips when Docker is absent).

One deployable: the same Spring Boot app serves the public website, the
signed-in web app, and (in future) the mobile API.

## Package layout (`com.emailmessenger`)

| Package | Responsibility |
|---|---|
| `domain` | JPA entities and enums (`User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `Plan`, `Team*`, `SavedSearch`, tokens, audit events). |
| `repository` | Spring Data repositories. `EmailThreadRepository` holds the JPQL that powers the conversation list, counts, and per-conversation queries. |
| `email` | Mailbox integration: `MailAccountService` (connect / delete / preferences / signature), `ImapClient` + `JakartaImapClient`, `MimeMessageParser`, `EmailImportService`, `MailboxPollingService` + `MailboxPollingScheduler`, `PollingPolicy`, IMAP IDLE (`ImapIdleService`, `ImapIdleSupport`, `JakartaImapIdleClient`), `GmailOAuthClient`, `CredentialEncryptor`, `OwnerAddressService`, `NewMailEvent`. |
| `service` | Conversation building and outbound mail: `ConversationKeyService`, `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP send), plus the view-model records (`ChatConversation`, `ChatMember`, `SenderBubbleRun`, `BubbleMessage`, `ConversationListItem`, `ThreadRef`, `SenderView`). |
| `web` | MVC controllers and web-side services: `ConversationController`, `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream`/`ChatEventsController` (SSE), `MailboxController`, `GmailConnectController`, saved searches, onboarding, landing/pricing/legal/SEO/PWA, `GlobalExceptionHandler`, model advices. |
| `auth` | `SecurityConfig`, registration, form + Google OIDC login, login throttle and audit, password reset, email verification, account settings. |
| `billing` | Stripe checkout / portal / webhook gateways, `BillingService`, `PlanLimits` + `PlanLimitService`, trial-end nudges, upgrade modal model. |
| `team` | Teams and invites. Behavioural detail **TBD**. |
| `admin` | Operator revenue dashboard and metrics services, win-back outreach, weekly operator digest. |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails (weekly digest, activation nudges, re-engagement). |

## Core domain rules

- **Threading:** messages group into an `EmailThread` by `Message-ID` /
  `In-Reply-To` / `References` headers.
- **Conversation key** (`ConversationKeyService`): the first 32 hex chars of
  SHA-256 over the thread's non-owner participant emails (senders + To/Cc,
  Bcc excluded), each trimmed and lowercased, de-duplicated, sorted, joined
  with `\n`. A thread whose only participant is the owner hashes the sentinel
  `" self"`. "Owner addresses" = the login email plus every connected
  mailbox username (`OwnerAddressService`). The `/chats` workspace merges all
  threads sharing a key into one timeline.
- **Plans** (`Plan`: `FREE`, `PRO`, `BUSINESS`; `PlanLimits`):

  | Plan | Mailboxes | Threads | Saved searches |
  |---|---|---|---|
  | FREE | 3 | 500 | unlimited |
  | PRO | 5 | unlimited | unlimited |
  | BUSINESS | unlimited | unlimited | unlimited |

  Exceeding a cap throws `PlanLimitExceededException` (`PlanLimitKind`:
  `THREAD_COUNT`, `MAILBOX_COUNT`, `SAVED_SEARCH_COUNT`). Only Pro has
  self-serve Stripe checkout (monthly + annual); Business is sales-assisted.
- **Polling** (`PollingPolicy`): Free every 15 min, paid every 5 min, ±30 s
  jitter; a user preference may slow but never beat the plan floor. Five
  consecutive failures suspend an account until a successful manual sync.
  Connecting a mailbox runs an initial sync of the latest 30 messages.
- **Live delivery:** non-Free accounts hold an IMAP IDLE connection
  (`mailbox.idle.enabled`). Any import publishes `NewMailEvent`, which
  `ChatEventStream` fans out over SSE after commit.
- **Outbound:** `ReplyService` sends over SMTP; `OutboundMessageService`
  records the sent message locally (marked outbound, thread marked read,
  conversation key pinned) so it appears immediately as a "you" bubble.
  Sent-folder import later syncs the provider's copy (V31 cursor).
- **Attachments:** metadata only. `attachments.blob_ref` is always null;
  bytes are not stored and cannot be downloaded.

## Database schema overview

| Table | Notes |
|---|---|
| `users` | account, password hash / `password_set`, `google_subject`, `email_verified_at`, activity + lifecycle-email stamps, `sync_frequency_minutes`, `default_mail_account_id`, `acquisition_source` |
| `mail_accounts` | per-user IMAP account; encrypted credentials; `auth_type` (`PASSWORD` or OAuth); `last_seen_uid`, `last_seen_sent_uid`; `next_poll_at`, `consecutive_failure_count`, `polling_suspended`; `signature` (≤ 2000) |
| `email_threads` | owner, subject, `root_message_id`, `message_count`, unread state, `conversation_key` (64) |
| `messages` | thread, sender participant, headers, plain + html body, `sent_at`, outbound flag |
| `message_recipients` | message ↔ participant with `recipient_type` (TO/CC/BCC) |
| `participants` | unique by email; display name |
| `attachments` | filename, mime, size, `blob_ref` (unused) |
| `subscriptions`, `plan_change_events` | Stripe subscription state, billing period, cancellation reason, trial-end / win-back email stamps |
| `saved_searches` | per-user saved filters |
| `teams`, `team_members`, `team_invites` | team membership and invites |
| `password_reset_tokens`, `email_verifications`, `digest_email_preferences`, `auth_events`, `persistent_logins` | auth support, opt-outs, audit, remember-me |

Migrations are immutable once merged; add `V36__…` for the next change.

## Auth model

- Spring Security, single filter chain (`auth/SecurityConfig`).
- Form login at `/login` (`email` / `password`), BCrypt, throttled by
  `LoginThrottleFilter` before credential check, audited via `auth_events`.
- Google OIDC login, enabled only when `GOOGLE_CLIENT_ID` is set.
- Server-side session + JDBC-backed 30-day remember-me.
- CSRF on, except `/billing/webhook` and `/h2-console/**`.
- Public routes are an explicit allowlist; everything else requires a session.
- `/admin/**` is gated by an email allowlist (`AdminAuthorizer`); non-admins
  receive a 404.
- Mailbox credentials are encrypted at rest (`CredentialEncryptor`,
  `MAILBOX_ENCRYPTION_PASSWORD` / `_SALT`).
- **No token/bearer auth exists.** A mobile client cannot authenticate today.

## Web pages vs. REST API

There is currently no REST API — only Thymeleaf pages plus two small
machine-readable endpoints the chats page itself uses (`POST /chats/refresh`,
SSE `GET /chats/events`). See [`API.md`](./API.md) for the contract and
[`WEB.md`](./WEB.md) for the page inventory.

Rule for all future work: REST controllers and Thymeleaf controllers must call
the **same service methods**. Known violations (business logic living in a
controller, which a REST endpoint would have to duplicate) are listed in
`INTERNAL_TODO.md`; the largest is the send path in `ConversationController`.

## Scheduled jobs

| Job | Default schedule |
|---|---|
| `MailboxPollingScheduler` | every 5 min (`mailbox.polling.interval-ms`), flag `mailbox.polling.enabled` |
| `ImapIdleService` reconcile | every 60 s, flag `mailbox.idle.enabled` |
| `ChatEventStream.keepalive` | every 25 s |
| `WeeklyDigestScheduler` | Mon 14:00 UTC |
| `AdminWeeklyDigestScheduler` | Mon 09:00 |
| `ActivationScheduler` | daily 13:30 / 13:45 / 14:00 UTC |
| `ReengagementScheduler` | daily 13:00 UTC |
| `TrialEndConversionScheduler` | daily 14:30 UTC |

## Configuration

Environment variables read by `application.yml`: `JDBC_DATABASE_URL`,
`JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`,
`MAIL_USER`, `MAIL_PASS`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`,
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `MAILBOX_ENCRYPTION_PASSWORD`,
`MAILBOX_ENCRYPTION_SALT`, `MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`.
Stripe price ids bind under `billing.stripe.*`. See `DEPLOY.md`.
