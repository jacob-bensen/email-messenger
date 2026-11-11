# Backend architecture

Reverse-engineered from the code as of migration `V36`. Anything not verified
in source is marked **TBD**. For the page-level view see [WEB.md](WEB.md); for
the JSON contract see [API.md](API.md).

- Spring Boot 3.5 (`spring-boot-starter-parent` 3.5.14), Java 21, Maven.
- Starters: web, thymeleaf, data-jpa, validation, mail, security,
  oauth2-client, actuator. Also jsoup, Jakarta Mail, `stripe-java` 32.1.0,
  Flyway (core + postgresql).
- Persistence is **Spring Data JPA / Hibernate** with
  `spring.jpa.hibernate.ddl-auto: validate` and `open-in-view: false`. Flyway
  owns the schema. There is no jOOQ.
- PostgreSQL at runtime; H2 (test scope only) for tests via
  `src/test/resources/application.yml`.
- `EmailMessengerApplication` enables `@EnableScheduling`, `@EnableAsync`,
  registers the `@ConfigurationProperties` classes, and exposes a
  `Clock.systemUTC()` bean.

## 1. Package layout

Root: `src/main/java/com/emailmessenger`

| Package | Responsibility | Key classes |
|---|---|---|
| `activation` | "Connect your mailbox" nudge emails for signups with no mail account (24 h, 72 h, 168 h after signup) | `ActivationService`, `ActivationScheduler` |
| `admin` | Operator dashboard metrics, win-back outreach, billing-period backfill, weekly operator digest. Gated by an email allowlist | `AdminRevenueController`, `AdminAuthorizer`, `AdminProperties`, `RevenueMetricsService`, `FunnelMetricsService`, `OnboardingFunnelMetricsService`, `ProAdoptionMetricsService`, `ChurnMetricsService`, `AtRiskRetentionService`, `TrialEndConversionMetricsService`, `WinBackConversionMetricsService`, `WinBackOutreachService`, `BillingPeriodBackfillService`, `AdminWeeklyDigestService`, `AdminWeeklyDigestScheduler` |
| `api` | JSON REST API under `/api/v1` (see section 7) | `AuthApiController`, `ConversationApiController`, `ConversationDtos`, `UserDto`, `ApiError`, `ApiExceptionHandler` |
| `auth` | Both security filter chains, registration, login audit/throttle, Google OIDC sign-in, password reset, email verification, account self-service, API tokens | `SecurityConfig`, `ApiSecurityConfig`, `ApiTokenAuthenticationFilter`, `ApiTokenService`, `UserService`, `DbUserDetailsService`, `LoginThrottleService`, `LoginThrottleFilter`, `AuthEventService`, `AuthFailureListener`, `LoginAuditListener`, `LoginFailureHandler`, `PlanCheckoutSuccessHandler`, `GoogleOAuthClientRegistrationConfig`, `GoogleOidcUserService`, `OAuth2ProvisioningService`, `OAuthIntentStore`, `PasswordResetService`, `EmailVerificationService`, `AccountService`, `UserActivityService`, `AppearanceSettings`, `ClientIp`; controllers `AuthController`, `AccountController`, `PasswordResetController`, `EmailVerificationController`, `OAuthStartController` |
| `billing` | Stripe checkout/portal/webhook, plan entitlements and caps, trial banners, trial-end email | `BillingService`, `BillingController`, `BillingWebhookController`, `BillingProperties`, `PlanLimitService`, `PlanLimits`, `PlanLimitExceededException`, `BillingBannerService`, `TrialEndConversionService`, `TrialEndConversionScheduler`, `TrialConversionNudgeService`, `UpgradeModal`, gateway interfaces + `Stripe*GatewayImpl` |
| `digest` | Weekly saved-search digest email and its unauthenticated opt-out | `WeeklyDigestService`, `WeeklyDigestScheduler`, `DigestController` |
| `domain` | JPA entities and enums | `User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `PlanChangeEvent`, `SavedSearch`, `DigestEmailPreference`, `PasswordResetToken`, `EmailVerificationToken`, `AuthEvent`, `ApiToken`, `Team`, `TeamMember`, `TeamInvite`; enums `Plan`, `AuthType`, `AuthEventType`, `RecipientType`, `CancellationReason`, `TeamMemberRole` |
| `email` | Mailbox connect, IMAP fetch, polling, IMAP IDLE, MIME parsing, import into threads, credential encryption, Gmail OAuth (XOAUTH2) | `MailAccountService`, `MailboxPollingService`, `MailboxPollingScheduler`, `PollingPolicy`, `ImapIdleService`, `ImapIdleSupport`, `ImapClient` / `JakartaImapClient`, `ImapIdleClient` / `JakartaImapIdleClient`, `ImapCredentialsResolver`, `CredentialEncryptor`, `GmailOAuthClient`, `EmailImportService`, `MimeMessageParser`, `OwnerAddressService`, `ConversationKeyBackfill`, `NewMailEvent` |
| `reengagement` | Email to users inactive for 7 days who have unread threads | `ReengagementService`, `ReengagementScheduler` |
| `repository` | Spring Data repositories (one per entity; `EmailThreadRepository` holds the conversation JPQL) | `EmailThreadRepository`, `MessageRepository`, `MailAccountRepository`, `UserRepository`, `SubscriptionRepository`, `ApiTokenRepository`, `AuthEventRepository`, ... |
| `service` | Conversation-key derivation, view-model assembly, body cleaning, outbound SMTP | `ConversationKeyService`, `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `DemoConversationService`; view models `ChatConversation`, `ChatMember`, `SenderBubbleRun`, `BubbleMessage`, `ConversationListItem`, `ThreadRef`, `SenderView`, `OutgoingAttachment` (plus older `Conversation`, `BubbleRun`, `SenderConversation`) |
| `team` | Owner-created team and emailed invites | `TeamService`, `TeamInviteService`, `TeamInviteController` |
| `web` | Thymeleaf controllers, controller advice, page-level services, PWA/SEO endpoints, SSE | `ConversationController`, `ConversationListService`, `ChatService`, `OutboundMessageService`, `ChatEventStream`, `ChatEventsController`, `ChatRedirectController`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `SavedSearchService`, `PricingController`, `DemoController`, `LegalController`, `SeoController`, `PwaController`, `NavModelAdvice`, `SiteModelAdvice`, `GlobalExceptionHandler`, `SiteProperties`, `LegalProperties`, `LandingProperties` |

Note: `ConversationListService` and `ChatService` are public services that
live in `web` but are consumed by `api` too.

## 2. Core flow

### 2.1 Connect a mailbox

| Step | Class | Notes |
|---|---|---|
| IMAP password connect | `MailboxController#createMailbox` -> `MailAccountService.connect` | Enforces mailbox cap, verifies the IMAP login, stores the password encrypted, imports the 30 most recent INBOX messages (`INITIAL_SYNC_LIMIT`). A failed initial fetch is recorded on the row, not thrown. |
| Gmail OAuth connect | `GmailConnectController` -> `GmailOAuthClient` -> `MailAccountService.connectGmailOAuth` | Hand-driven authorization-code flow (scope `https://mail.google.com/` + userinfo email), state kept in the HTTP session. Stores the encrypted refresh token with `auth_type = XOAUTH2`; host fixed to `imap.gmail.com:993`. |
| Gmail reconnect | `GmailConnectController#reconnect` -> `MailAccountService.reconnectGmailOAuth` | Replaces the stored token in place; authorized address must equal the mailbox username. |
| Credentials at rest | `CredentialEncryptor` | Spring Security `Encryptors.delux` keyed by `mailbox.encryption.password` / `.salt`; falls back to fixed dev constants with a warning when unset. |
| Credentials at use | `ImapCredentialsResolver` | Decrypts; for XOAUTH2 exchanges the refresh token for an access token on every call and writes back a rotated refresh token. |

### 2.2 Import (IMAP -> threads)

`MailboxPollingService.pollOne(accountId)` is the single import path. Callers:

| Trigger | Entry point | Selection |
|---|---|---|
| Background schedule | `MailboxPollingScheduler` -> `pollAll()` | `findDueForPolling`: not suspended and `next_poll_at` null or past |
| Page open | `ConversationController` GET `/chats`, `/chats/{key}` -> `refreshDueForUserAsync` (`@Async`) | Same rule, one user |
| Foreground heartbeat | POST `/chats/refresh` -> `refreshActiveUserNow` | Not suspended and `last_synced_at` older than 1 minute |
| Manual sync / reconnect | `MailboxController#syncMailbox`, `GmailConnectController` | Direct `pollOne` |
| IMAP IDLE | `ImapIdleService` worker | On connect and on each new-mail signal |

`pollOne`:

1. Resolve credentials, `ImapClient.fetchSinceUid(lastSeenUid)` on INBOX,
   import each message as inbound, advance `last_seen_uid`.
2. Sent folder: first run (`last_seen_sent_uid` null) backfills the 30 most
   recent sent messages, then tracks the cursor; all imported with
   `outbound = true`. Folder name is probed from `[Gmail]/Sent Mail`,
   `Sent Items`, `Sent`, `Sent Mail`, `INBOX.Sent`.
3. On `ImapConnectionException`: store `last_sync_error`, increment the
   failure count, suspend polling after 5 consecutive failures
   (`PollingPolicy.SUSPEND_AT_FAILURES`).
4. Stamp `next_poll_at` via `PollingPolicy` (Free 15 min, paid 5 min, user
   preference can only slow it, +/-30 s jitter).
5. If anything was imported, publish `NewMailEvent(userId, count)`.

`EmailImportService.importMessage(mime, owner, outbound)`:

- Parses with `MimeMessageParser`.
- Idempotent per owner on `Message-ID`.
- Thread resolution, all owner-scoped: `References` newest-first, then
  `In-Reply-To`, then `root_message_id`; otherwise a new `EmailThread`
  after `PlanLimitService.enforceCanCreateThread`.
- Resolves/creates `Participant` rows by email; stores TO/CC/BCC recipients
  and attachment metadata (`blob_ref` is always null; bytes are not stored).
- Inbound messages mark the thread unread; outbound ones do not.
- Recomputes `email_threads.conversation_key` from the thread's full
  participant set.

### 2.3 conversationKey

`ConversationKeyService`:

- Members = every message sender + TO/CC recipients, minus the owner's own
  addresses (`OwnerAddressService`: login email + every connected mailbox
  username), lowercased, de-duplicated, sorted. BCC is excluded.
- Key = first 32 hex chars of SHA-256 over the newline-joined members; a
  fixed sentinel is hashed when only the owner is involved.
- `compute(thread, ownerAddresses)` is used at import;
  `keyFor(emails, ownerAddresses)` is used when composing.
- `ConversationKeyBackfill` fills null keys once at `ApplicationReadyEvent`.

A conversation is therefore "all threads of one owner sharing a key", not a
table of its own.

### 2.4 Chat view models

| Need | Service | Output |
|---|---|---|
| Conversation list (rail) | `web.ConversationListService.list(owner, mailboxAddress, query, ChatFilter, pageable)` | `Page<ConversationListItem>`; grouped JPQL in `EmailThreadRepository.conversationSummariesFiltered`, then previews/members fetched per page. Filters: `ALL`, `UNREAD`, `AWAITING`, `ATTACHMENTS`. Account tags only when unscoped. |
| Tile counts | `ConversationListService.counts(owner, mailboxAddress)` | `ChatCounts(all, unread, awaiting, attachments)` |
| One conversation | `web.ChatService.buildFor(owner, key)` / `buildFor(owner, mailboxAddress, key)` | `ChatConversation`, built by `service.ConversationService.buildChatConversation` |
| Mark read | `ChatService.markRead(owner, mailboxAddress, key)` | Sets `unread = false` on the key's threads |

`ConversationService` groups messages into `SenderBubbleRun`s (new run on
thread change or direction flip), resolves in-view reply targets from
`in_reply_to`, and renders each body: HTML bodies through `EmailBodyCleaner`
(strip quoted history and signatures, sanitize), plain bodies through
`IMTransformService` (strip quotes, render markdown). The latest detected
signature per member is attached to `ChatMember.signatureHtml`.

### 2.5 Reply / send

Orchestrated in `web.ConversationController` (see section 9):

1. Resolve the sending account: mailbox-scoped view, else the user's default
   account, else the sole account, else none (From = login email).
2. Append the account signature under `-- `.
3. `service.ReplyService`:
   - `sendReply(threadId, subject, body, attachments, recipients, from)`
     sets `In-Reply-To`/`References` to the **last message of the thread**.
   - `sendNewEmail(subject, body, attachments, recipients, from)` sends
     without threading headers.
   - Both send through the application-wide `JavaMailSender`
     (`spring.mail.*`), not through the user's own mailbox SMTP.
4. `web.OutboundMessageService` (package-private) records the sent message
   locally as an outbound `Message`: `recordReply` appends to the thread;
   `recordNewEmail` creates a new thread pinned to the conversation key with
   a locally generated `Message-ID` (`<uuid@conexusmail.com>`).

### 2.6 Live delivery

`NewMailEvent` -> `web.ChatEventStream` (`@TransactionalEventListener`,
after commit, with fallback execution) -> SSE event `new-mail` with
`{"imported":N}` to every open emitter of that user. Emitters are held in
memory per JVM (no cross-instance fan-out). A keep-alive comment is sent
every `mailbox.sse.keepalive-ms` (default 25 s).

## 3. Database schema

Migrations: `src/main/resources/db/migration/V1..V36`. `db/init.sql` only
creates the database and app user.

| Table | Added | Holds | Key relations / constraints |
|---|---|---|---|
| `participants` | V1 | One row per email address (global, not per user); display name | unique `email` |
| `email_threads` | V1, V3, V9, V32 | Subject, `root_message_id`, `message_count`, `unread`, `conversation_key` | `owner_id` -> `users`; unique (`owner_id`, `root_message_id`); index (`owner_id`, `conversation_key`) |
| `messages` | V1, V30 | `message_id_header`, `in_reply_to`, subject, plain/HTML body, `sent_at`, `outbound` | `thread_id` -> `email_threads`; `sender_id` -> `participants` |
| `message_recipients` | V1 | TO/CC/BCC rows | `message_id` -> `messages`; `participant_id` -> `participants` |
| `attachments` | V1 | Filename, mime type, size, `blob_ref` (unused) | `message_id` -> `messages` |
| `users` | V2 + V8, V13, V15, V18-V22, V34 | Login email, BCrypt hash, `password_set`, `google_subject`, `email_verified_at`, acquisition source, activity/nudge timestamps, `sync_frequency_minutes`, `default_mail_account_id` | unique `email`, unique `google_subject`; `default_mail_account_id` -> `mail_accounts` (on delete set null) |
| `persistent_logins` | V2 | Spring Security remember-me tokens (no JPA entity; `JdbcTokenRepositoryImpl`) | pk `series` |
| `subscriptions` | V4, V17, V23, V27, V28 | Plan, Stripe status and ids, trial/period end, billing period, cancellation reason, trial-end and win-back email stamps | unique `user_id`; unique `stripe_subscription_id` |
| `mail_accounts` | V5-V7, V29, V31, V35 | IMAP host/port/ssl/username, encrypted secret, `auth_type`, UID cursors (inbox + sent), sync status, failure count, `polling_suspended`, `next_poll_at`, signature | `user_id` -> `users`; unique (`user_id`, `host`, `username`) |
| `saved_searches` | V10, V11 | Named filter (query, sender, since preset, unread, attachments), `last_viewed_at` | `owner_id` -> `users`; unique (`owner_id`, `name`) |
| `digest_email_preferences` | V12 | Opt-out token and flag shared by all automated emails, `last_sent_at` | unique `user_id`; unique `opt_out_token` |
| `password_reset_tokens` | V14 | SHA-256 token hash, expiry (1 h), `used_at` | `user_id` -> `users`; unique `token_hash` |
| `email_verifications` | V15 | SHA-256 token hash, expiry (24 h), `used_at` | `user_id` -> `users`; unique `token_hash` |
| `auth_events` | V16 | Login success/failure, lockout, password/email change; email + IP | `user_id` -> `users` (nullable); indexes on user, email, ip with `created_at` |
| `teams` | V24 | Team name | unique `owner_user_id` -> `users` |
| `team_members` | V24 | Membership and role | `team_id`, `user_id`; unique pair |
| `team_invites` | V24 | Invitee email, token hash, expiry (7 d), accepted/revoked stamps | `team_id`, `invited_by_user_id` |
| `plan_change_events` | V26 | Plan transition log for admin metrics | `subscription_id`, `user_id` |
| `api_tokens` | V36 | SHA-256 hash of an API bearer token, expiry, `revoked_at` | `user_id` -> `users`; unique `token_hash` |

`thread_notes` was created in V25 and dropped in V33.

Timestamps are `TIMESTAMP` without zone. Most code writes
`LocalDateTime.now(clock)` (UTC); `MailAccount.markSynced()` uses the system
zone (noted in `MailboxPollingService.refreshActiveUserNow`).

Multi-tenancy is by `owner_id` / `user_id` on every query; `participants` is
the only shared table.

## 4. Auth model

Two `SecurityFilterChain` beans. The principal name is the user's email on
both, so controllers resolve the user identically via
`UserService.requireByEmail(principal.getName())`.

| | API chain | Website chain |
|---|---|---|
| Class | `auth/ApiSecurityConfig` (`@Order(1)`) | `auth/SecurityConfig` (default order) |
| Matcher | `/api/**` | everything else |
| Credential | `Authorization: Bearer <token>` | `JSESSIONID` session cookie, `remember-me` cookie |
| Session | `STATELESS`, request cache disabled | Servlet session |
| CSRF | Disabled | Enabled; ignored for `/h2-console/**` and `/billing/webhook` |
| Open paths | `POST /api/v1/auth/login` | See WEB.md section 1 |
| Unauthenticated | `401` JSON `{"error":{"code":"unauthorized",...}}` | Redirect to `/login` |
| Login | `ApiTokenService.login` | Form login (`POST /login`, params `email`, `password`), Google OIDC |

### API tokens

`ApiTokenService` + `api_tokens`:

- `login(email, password, ip)`: checks the throttle, verifies BCrypt
  (accounts with `password_set = false` cannot log in this way), issues a
  32-byte URL-safe random token, stores only its SHA-256 hex, TTL 90 days.
- `resolveEmail(token)`: token must be unexpired, unrevoked, and its user
  enabled. Called per request by `ApiTokenAuthenticationFilter`.
- `revoke(token)`: sets `revoked_at`.
- No refresh endpoint and no token listing exist.

### Website login

- `DbUserDetailsService` loads by normalized email; no authorities/roles.
- Remember-me: persistent tokens in `persistent_logins`, 30 days, parameter
  `remember-me`.
- Logout: `POST /logout` -> `/login?logout`.
- Success: `PlanCheckoutSuccessHandler` (default target `/threads`, which
  redirects to `/chats`; if a `plan` param or stored OAuth intent is
  present, redirects to Stripe Checkout).
- Failure: `LoginFailureHandler` -> `/login?error` or `/login?error=google`.
- Google OIDC is enabled only when `auth.google.client-id` and
  `client-secret` are both set (`GoogleOAuthClientRegistrationConfig`).
  `GoogleOidcUserService` + `OAuth2ProvisioningService` find-or-create the
  `users` row and make the principal name the email.
  `OAuthStartController` + `OAuthIntentStore` carry `plan`/`billing`/
  `utm_source` across the redirect in the session.
- Admin: no role. `AdminAuthorizer.requireAdmin(email)` checks
  `admin.emails` and throws `NoSuchElementException` (rendered as 404).

### Shared throttle and audit

- `AuthEventService` writes `auth_events`.
- `LoginThrottleService.isLocked(email, ip)`: locked when
  `LOGIN_FAILURE` rows in the window reach the limit for the email **or**
  the IP (defaults: 5 failures / 15 minutes; `auth.throttle.max-failures`,
  `auth.throttle.window-minutes`). No stored lock flag.
- Website: `LoginThrottleFilter` short-circuits `POST /login` to
  `/login?error=locked`; `AuthFailureListener` and `LoginAuditListener`
  record failures/successes from Spring Security events.
- API: `ApiTokenService.login` calls the same throttle and records the same
  event types directly, so both surfaces share one lockout window.
- Client IP: `ClientIp` takes the first `X-Forwarded-For` value, else the
  remote address.

## 5. Scheduled jobs

| Job | Class | Schedule (default) | Enabled when | Default in `application.yml` |
|---|---|---|---|---|
| Mailbox poll | `email.MailboxPollingScheduler` | fixed delay `mailbox.polling.interval-ms` (300000), initial `mailbox.polling.initial-delay-ms` (30000) | `mailbox.polling.enabled=true` | on (`MAILBOX_POLLING_ENABLED`, default `true`) |
| IMAP IDLE reconcile | `email.ImapIdleService` | fixed delay `mailbox.idle.reconcile-interval-ms` (60000), initial `mailbox.idle.initial-delay-ms` (15000) | `mailbox.idle.enabled=true` | on (`MAILBOX_IDLE_ENABLED`, default `true`) |
| SSE keep-alive | `web.ChatEventStream#keepalive` | fixed delay `mailbox.sse.keepalive-ms` (25000) | always | - |
| Activation nudge | `activation.ActivationScheduler` | `activation.cron` (`0 30 13 * * ?`) | `activation.enabled=true` | off (not set) |
| Activation follow-up | same | `activation.followup.cron` (`0 45 13 * * ?`) | same | off |
| Activation last chance | same | `activation.lastchance.cron` (`0 0 14 * * ?`) | same | off |
| Re-engagement | `reengagement.ReengagementScheduler` | `reengagement.cron` (`0 0 13 * * ?`) | `reengagement.enabled=true` | off |
| Weekly digest | `digest.WeeklyDigestScheduler` | `digest.cron` (`0 0 14 ? * MON`) | `digest.enabled=true` | off |
| Trial-end email | `billing.TrialEndConversionScheduler` | `trial-end.cron` (`0 30 14 * * ?`) | `trial-end.enabled=true` | off |
| Admin weekly digest | `admin.AdminWeeklyDigestScheduler` | `admin.weekly-digest.cron` (`0 0 9 ? * MON`) | `admin.weekly-digest.enabled=true` | off |

Cron zones default to UTC (`<prefix>.zone`). The services behind the
schedulers are always in the context; only the triggers are conditional.

IMAP IDLE holds one daemon thread and connection per eligible mailbox
(not suspended, owner on a paid plan), with linear backoff from 30 s up to
10 min on failure. One non-scheduled startup task also exists:
`ConversationKeyBackfill` runs once on `ApplicationReadyEvent`.

No distributed lock exists on any job (single-instance assumption).

## 6. Configuration

### Declared in `application.yml`

| Property | Env var | Default |
|---|---|---|
| `spring.datasource.url` / `username` / `password` | `JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` | none (required) |
| `spring.mail.host` / `port` / `username` / `password` | `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASS` | `localhost` / `587` / empty / empty; SMTP auth + STARTTLS on |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | - | `10MB` / `25MB` |
| `auth.google.client-id` / `client-secret` | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | empty (Google sign-in and Gmail OAuth off) |
| `billing.stripe.secret-key` / `webhook-secret` | `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | empty |
| `mailbox.encryption.password` / `salt` | `MAILBOX_ENCRYPTION_PASSWORD` / `MAILBOX_ENCRYPTION_SALT` | empty (dev fallback key) |
| `mailbox.polling.enabled` | `MAILBOX_POLLING_ENABLED` | `true` |
| `mailbox.idle.enabled` | `MAILBOX_IDLE_ENABLED` | `true` |
| `admin.emails` | - | hardcoded single operator address |
| `management.health.mail.enabled` | - | `false` |
| `server.compression.*`, `server.error.whitelabel.enabled=false`, logging levels | - | see file |

### Read by code but not declared in `application.yml`

These have in-code defaults and no explicit `${ENV}` placeholder in the main
config. They can be supplied through Spring Boot relaxed binding (for example
`BILLING_STRIPE_PRO_PRICE_ID`). The short env names in
`src/test/resources/application.yml` (for example `STRIPE_PRO_PRICE_ID`,
`MARKETING_BASE_URL`, `DIGEST_ENABLED`) are wired only in that test file,
although several of those short names also happen to match the relaxed form.

| Property | Default | Bound by |
|---|---|---|
| `billing.stripe.pro-price-id`, `pro-annual-price-id` | empty (checkout throws `BillingException`) | `BillingProperties` |
| `billing.stripe.success-url`, `cancel-url`, `portal-return-url` | empty | `BillingProperties` |
| `billing.stripe.trial-days` | `14` | `BillingProperties` |
| `auth.google.redirect-uri` | `{baseUrl}/login/oauth2/code/{registrationId}` | `GoogleOAuthProperties` |
| `auth.throttle.max-failures`, `window-minutes` | `5`, `15` | `LoginThrottleService` |
| `marketing.base-url` | `https://conexusmail.com` | `SiteProperties` |
| `marketing.legal.privacy` / `terms` / `refund` | `classpath:legal/*.html` | `LegalProperties` |
| `marketing.landing.video.provider` / `id` / `poster-url` / `title` | empty / empty / empty / `ConexusMail demo` | `LandingProperties` |
| `mailbox.polling.interval-ms`, `initial-delay-ms` | `300000`, `30000` | `MailboxPollingScheduler` |
| `mailbox.idle.reconcile-interval-ms`, `initial-delay-ms` | `60000`, `15000` | `ImapIdleService` |
| `mailbox.sse.keepalive-ms` | `25000` | `ChatEventStream` |
| `activation.*`, `reengagement.*`, `digest.*`, `trial-end.*`, `admin.weekly-digest.*` (`enabled`, `cron`, `zone`) | disabled; crons in section 5 | schedulers |

The fallback From address for system email is `spring.mail.username`, else
`noreply@conexusmail.com`.

### `application-local.yml` (`local` profile)

| Setting | Value |
|---|---|
| Datasource | `jdbc:postgresql://localhost:5432/email_messenger`, user `email_messenger_app` (password still from `JDBC_DATABASE_PASSWORD`) |
| Thymeleaf cache | off |
| SMTP | `localhost:1025`, no auth, no STARTTLS |
| Stripe URLs | `http://localhost:8080/billing/success?session_id={CHECKOUT_SESSION_ID}`, `/billing/cancel`, portal return `/threads` |
| `marketing.base-url` | `http://localhost:8080` |
| Logging | `com.emailmessenger: DEBUG` |

There is no `dev` or `prod` profile file.

### Plan caps (`billing.PlanLimits`, in code)

| Plan | Mailboxes | Threads | Saved searches | Poll floor | IMAP IDLE |
|---|---|---|---|---|---|
| FREE | 3 | 500 | unlimited | 15 min | no |
| PRO | 5 | unlimited | unlimited | 5 min | yes |
| BUSINESS | unlimited | unlimited | unlimited | 5 min | yes |

Effective plan = the subscription's plan when its Stripe status is `active`
or `trialing`, otherwise FREE (`PlanLimitService.currentPlan`). Only PRO has
a self-serve checkout; `BillingService.startCheckout` rejects BUSINESS.

Stripe webhook events handled by `BillingService.applyStripeEvent`:
`checkout.session.completed`, `customer.subscription.created`,
`customer.subscription.updated`, `customer.subscription.deleted`. Others are
ignored.

## 7. Thymeleaf pages and the REST API

The `com.emailmessenger.api` package is a thin JSON projection over the same
services the pages call. It contains no business rules; `ConversationDtos`
only maps view-model records to wire records (timestamps converted from UTC
`LocalDateTime` to `Instant`).

| API endpoint | Controller | Service call | Page equivalent |
|---|---|---|---|
| `POST /api/v1/auth/login` | `AuthApiController` | `auth.ApiTokenService.login` | `POST /login` (form login) |
| `POST /api/v1/auth/logout` | `AuthApiController` | `ApiTokenService.revoke` | `POST /logout` |
| `GET /api/v1/me` | `AuthApiController` | `auth.UserService.requireByEmail`, `billing.PlanLimitService.currentPlan` | `/account` |
| `GET /api/v1/conversations` | `ConversationApiController` | `web.ConversationListService.list` + `.counts` | `GET /chats` |
| `GET /api/v1/conversations/{key}` | `ConversationApiController` | `web.ChatService.buildFor(owner, key)` | `GET /chats/{key}` |
| `POST /api/v1/conversations/{key}/read` | `ConversationApiController` | `ChatService.markRead(owner, null, key)` | implicit on `GET /chats/{key}` |

Differences from the pages:

- The API is always the unscoped (cross-account) view; the `mailbox`
  scoping parameter is not exposed.
- `GET /api/v1/conversations/{key}` does not mark the conversation read and
  does not trigger a mailbox refresh; the page does both.
- Page size is caller-controlled on the API (default 30, max 100); the page
  uses a fixed 30.
- An unknown `filter` is a 400 on the API and silently `ALL` on the page.
- Errors: `ApiExceptionHandler` (`@RestControllerAdvice` scoped to the
  `api` package, highest precedence) returns the `ApiError` envelope;
  pages use `web.GlobalExceptionHandler` and the `error` template.
- Sending, composing, mailbox management, billing, saved searches and teams
  have no API endpoints.

The request/response contract is specified in [API.md](API.md).

## 8. Other notes

- Attachments: metadata only. No download endpoint; `blob_ref` is never set.
- Outbound uploads are read fully into memory (`OutgoingAttachment` holds a
  `byte[]`), bounded by the multipart limits.
- Actuator: only `/actuator/health` is permitted by the security chain.
- Automated emails (activation, re-engagement, digest, trial-end, win-back)
  share the `digest_email_preferences` opt-out.

## 9. Known structural debt

Observed in the current code; each item blocks or complicates exposing the
feature through the API.

| Area | Where | Issue |
|---|---|---|
| Send / compose orchestration | `web/ConversationController#send`, `#createMessage` | Validation, recipient parsing, sender-account choice (`sendingAccount`), signature append (`withSignature`), reply-vs-new decision, SMTP send and local recording are all sequenced in the controller. No single service method represents "send a message in conversation X". |
| Send is not atomic | same | `ReplyService` (SMTP) and `OutboundMessageService` (DB) are two separate calls with no shared transaction or compensation. |
| Outbound recording is package-private | `web/OutboundMessageService` | Not callable from `api`; lives in `web` although it is domain logic. |
| Mailbox scoping helpers | `ConversationController#selectMailbox`, `#scopeAddress`, `#address`, `#mailboxQuery` | The "which account is this view scoped to" rule (only scope when more than one account) is private static controller code. |
| Presentation helpers | `ConversationController#accountColorMap`, `#fallbackAccount` | Computed in the controller and put in the model. |
| Mailbox preferences validation | `web/MailboxController#updatePreferences`, `#saveSignature` | Allowed sync frequencies, plan-floor check and signature length limit are enforced in the controller; `MailAccountService.updateMailPreferences` trusts its caller. |
| Controllers using repositories directly | `MailboxController`, `GmailConnectController` (`MailAccountRepository`), `ConversationController` (`EmailThreadRepository`), `AccountController` (`SubscriptionRepository`), `TeamInviteController` (`TeamInviteRepository`), `DigestController` (`DigestEmailPreferenceRepository`, with `@Transactional` on the handler) | Ownership checks and lookups bypass the service layer. |
| Gmail OAuth flow | `web/GmailConnectController` | State handling, token validation and the reconnect address check live in the controller and depend on the HTTP session. |
| Shared services in `web` | `web.ConversationListService`, `web.ChatService` | Used by `api`; `api` therefore depends on the `web` package. |
| Reads with side effects | `ConversationController#chat`, `#chatPane` | GET marks the conversation read and kicks off an async poll. |
| Controller advice scope | `web.NavModelAdvice`, `web.SiteModelAdvice` | Not restricted to page controllers, so their `@ModelAttribute` methods (user, mailbox list and billing lookups) also run for `/api/**`, `/chats/refresh` and `/billing/webhook` requests. |
| Reply threading target | `service.ReplyService#sendReply` | Always threads onto the thread's last message; the controller always picks the most recently active thread of the conversation. |
| Plan-limit handling in polling | `MailboxPollingService#pollOne` | Only `EmailImportException` and `ImapConnectionException` are caught; a `PlanLimitExceededException` from `EmailImportService` aborts the poll before cursors are saved. |
| Legacy code paths | No callers in `src/main`: `EmailThreadRepository.topSenders`, `findConversationWithSender`, `latestInboundSenders`; `ConversationService.buildSenderConversation`; `web.OnboardingService`; `web.OnboardingNudge`; `billing.TrialConversionNudgeService`; `UserActivityService.recordInboxVisit`. Only reached by non-page code: `findByOwnerFiltered` / `searchIncludingBody` (weekly digest), `buildConversation` (demo page), `web.SavedSearchCountService` (only via `SavedSearchService`, whose read side no controller calls) | Remnants of the retired `/threads` and `/senders` views. |
