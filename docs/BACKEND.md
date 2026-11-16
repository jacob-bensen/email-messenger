# ConexusMail backend — architecture

Spring Boot 3.5 / Java 21. One deployable serves:
- the Thymeleaf website (`docs/WEB.md`);
- the mobile API contract (`docs/API.md`);
- the background mail jobs.

It owns all persistence and every authoritative business rule.
Reverse-engineered from the code on 2026-11-16.

## Stack
- Data and persistence: Spring Data **JPA** (Hibernate, `ddl-auto: validate`)
  over PostgreSQL 16, with Flyway migrations. There is no jOOQ.
- Web and security: Spring MVC with Thymeleaf. Spring Security provides
  session auth, remember-me, and optional Google OIDC.
- Mail and parsing: Jakarta Mail for IMAP, IMAP IDLE and SMTP. Jsoup does the
  HTML cleanup.
- Billing: stripe-java.
- Tests: JUnit 5 with H2. One end-to-end test uses Testcontainers
  (Postgres) and GreenMail, and is skipped when Docker is absent.

## Packages (`com.emailmessenger`)
| Package | Responsibility |
|---|---|
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, MessageRecipient, Participant, Attachment, Subscription, SavedSearch, Team*, tokens, events) and enums (Plan, AuthType, RecipientType, …) |
| `repository` | One Spring Data repository per entity |
| `email` | IMAP ingest: polling (`MailboxPollingService`/`Scheduler`, `PollingPolicy`), IDLE (`ImapIdleService`), import + threading (`EmailImportService`, `MimeMessageParser`), credentials (`CredentialEncryptor` AES-GCM, `GmailOAuthClient` XOAUTH2), `MailAccountService`, `OwnerAddressService`, `ConversationKeyBackfill`, `NewMailEvent` |
| `service` | Rendering rules: `ConversationKeyService`, `ConversationLabels`, `ConversationService` (bubble runs), `EmailBodyCleaner` (HTML), `IMTransformService` (plain text), `ReplyService` (SMTP send), demo data |
| `web` | Thymeleaf controllers, plus web-layer services that the API must reuse: `ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `OnboardingService`. Also `ChatEventStream` (SSE) and the model advice classes |
| `auth` | `SecurityConfig`; registration, account, password reset and email verification; Google OIDC provisioning; login throttle and audit (`auth_events`) |
| `billing` | Stripe checkout, portal and webhooks (`BillingService`, gateways); entitlements (`PlanLimitService`, `PlanLimits`, `PlanLimitKind`); trial banners and nudges |
| `team` | One owned team per user; hashed 7-day invites |
| `digest`, `activation`, `reengagement`, `admin` | Lifecycle emails and the operator metrics dashboard (`/admin/revenue`) |

Some services live in `web/`, for example `ChatService` and
`ConversationListService`. Before the API reuses them they could move to
`service/`. That move is optional; reusing the class is what matters.

## Data model (after V35)
- **users** hold login fields (email, password hash, `password_set`,
  `google_subject`, `email_verified_at`), lifecycle nudge stamps and mail
  preferences (`sync_frequency_minutes`, `default_mail_account_id`).
- **subscriptions** hold one row per user: plan, status and Stripe ids. They
  also record the trial and period end, the billing period, the cancellation
  reason and win-back stamps.
- **mail_accounts** belong to a user. Each holds host/port/ssl/username and
  an encrypted secret (a password or a Gmail refresh token, chosen by
  `auth_type`). It also keeps the UID cursors (`last_seen_uid`,
  `last_seen_sent_uid`), the polling breaker state and a signature.
- **email_threads** are owned by a user, unique on (owner, root Message-ID).
  Each has an `unread` flag and a `conversation_key`.
- **messages** belong to a thread: Message-ID, In-Reply-To, sender (a
  participant), plain and HTML body, `sent_at`, `outbound`.
  - **message_recipients** link a message to participants with a type
    (TO, CC or BCC).
  - **attachments** hold metadata only; `blob_ref` is always null.
- **participants** are global and keyed by normalized email.
- Smaller tables: saved_searches, digest_email_preferences,
  password_reset_tokens, email_verifications, auth_events, persistent_logins,
  teams, team_members, team_invites and plan_change_events.

**Conversation** is not a table. It is every thread an owner has with the same
`conversation_key`, the hash of the participant set (`API.md` §4.1). The
website and the mobile app both address a chat by this key.

## Auth model
- Session-based. Users sign in with an email and a BCrypt password through
  form login, or with Google OIDC. Remember-me cookies are backed by JDBC
  and last 30 days.
- CSRF protection is on everywhere except `/billing/webhook` and
  `/h2-console/**`.
- Throttling: after 5 failed logins per email or IP within 15 minutes, the
  account is locked out (`auth.throttle.*`).
- Email verification is advisory: unverified users see a banner but nothing
  is blocked.
- Admin access is an email allowlist (`admin.emails`). Non-admins get a 404.
- The mobile client has no auth today. The proposed design is in
  `API.md` §5.

## Plans and limits
- Plans are `FREE`, `PRO` and `BUSINESS`. The caps are in `API.md` §4.3.
- `PlanLimitService.currentPlan` returns the subscription's plan when its
  status is `active` or `trialing`. Otherwise the user is on FREE.
- The caps are enforced in the services, not in controllers:
  - the thread cap in `EmailImportService` (new threads only);
  - the mailbox cap in `MailAccountService`;
  - the saved-search cap in `SavedSearchService`.
- Polling cadence also depends on the plan. Free accounts are polled every
  15 minutes and paid accounts every 5. IMAP IDLE runs for paid accounts
  only.

## Mail pipeline
1. **Fetch.** `MailboxPollingService.pollOne(account)` fetches INBOX
   messages with UIDs past the cursor. It then fetches the Sent folder:
   the first run backfills the 30 most recent messages, later runs are
   incremental, and sent mail is imported as `outbound`.
2. **Import.** `EmailImportService` threads each message in this order:
   References (newest first), then In-Reply-To, then the root Message-ID,
   and otherwise creates a new thread (checking the thread cap). It
   deduplicates per owner by Message-ID, then recomputes the conversation key.
3. **Notify.** After commit, `NewMailEvent` reaches `ChatEventStream`, which
   pushes SSE `new-mail` events to open `/chats` tabs.
4. **Render.**
   - `EmailBodyCleaner` and `IMTransformService` strip quoted replies and
     lift out signatures.
   - `ConversationService`, `ChatService` and `ConversationListService`
     build the bubble runs and the list rows.
5. **Send.**
   - `ReplyService` sends over SMTP. A reply threads onto the thread's last
     message.
   - `OutboundMessageService` records the sent message locally so it shows
     up immediately.

## Scheduled jobs
| Job | Default schedule | Flag |
|---|---|---|
| Mailbox polling | every 5 min | `mailbox.polling.enabled` (on) |
| IMAP IDLE reconcile | every 60 s | `mailbox.idle.enabled` (on) |
| SSE keepalive | every 25 s | always on |
| Weekly digest | Mon 14:00 UTC | `digest.enabled` (off) |
| Re-engagement | daily 13:00 | `reengagement.enabled` (off) |
| Activation drip (3 crons) | daily 13:30 / 13:45 / 14:00 | `activation.enabled` (off) |
| Trial-end conversion | daily 14:30 | `trial-end.enabled` (off) |
| Admin weekly digest | Mon 09:00 | `admin.weekly-digest.enabled` (off) |

`ConversationKeyBackfill` runs once at startup.

## Web pages vs. API
Both must call the same services. Today some rules still live in
controllers, most importantly the compose and reply flow in
`ConversationController`. Those rules are listed in `docs/WEB.md` and
`INTERNAL_TODO.md`. They must move into services before the matching API
endpoint is built.

## Configuration
- `application.yml` reads `JDBC_DATABASE_*` and `MAIL_*` from the
  environment.
- Custom property prefixes: `auth.*`, `billing.stripe.*`, `mailbox.*`,
  `admin.*`, `digest.*`, `reengagement.*`, `activation.*`, `trial-end.*`,
  `marketing.*`.
- `application-local.yml` is the only profile and points at a local Postgres.
- Tests use H2 (`src/test/resources/application.yml`).

## Legacy notes
`master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS) belongs to an earlier
planning routine and was last updated in 2026-06.
- Its pricing text is out of date. It describes Personal/Team/Enterprise;
  the code has Free/Pro/Business.
- Its `MASTER_ACTIONS.md` still lists useful ops TODOs (deploy flags,
  credentials).
- The current run log is the root `CHANGELOG.md`.
