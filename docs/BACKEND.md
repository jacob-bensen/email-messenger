# ConexusMail Backend — Architecture

Spring Boot 3 / Java 21 monolith. Serves two surfaces:

1. **The website** — Thymeleaf pages under `templates/`, session-cookie
   auth (`SecurityConfig`'s form-login chain). See `docs/WEB.md`.
2. **The JSON API** — `/api/v1/**`, bearer-token auth (see `docs/API.md`).
   Consumed by the Flutter client in `jacob-bensen/conexus-mail-mobile`.

Both surfaces share the same service layer. No business logic lives in
controllers or templates — a controller extracts inputs, calls services,
and formats the response.

## Package map

```
com.emailmessenger
├── EmailMessengerApplication.java     # @SpringBootApplication + @EnableScheduling
├── auth/                              # login, registration, password reset, email
│                                      # verification, Google OIDC, account settings,
│                                      # SecurityConfig, LoginThrottleFilter
├── admin/                             # /admin/** operator dashboard
├── activation/                        # day-1 mailbox-connect nudge
├── reengagement/                      # 7-day inactivity re-engagement
├── digest/                            # weekly digest emails
├── billing/                           # Stripe checkout, portal, webhook, plan changes
├── domain/                            # JPA entities (see §Entities)
├── email/                             # IMAP polling, IDLE, RFC-822 parsing, thread
│                                      # building, outgoing SMTP via ReplyService
├── repository/                        # Spring Data JPA repositories
├── service/                           # ConversationService, IMTransformService,
│                                      # ReplyService, DemoConversationService
├── team/                              # team invites and membership
└── web/                               # Thymeleaf controllers + the chat UI, plus
                                       # ChatEventStream (SSE) and PWA endpoints
```

The mobile-facing REST API lives in a new sub-package under `web/api/**`
(introduced 2026-08-28); see `docs/API.md` for the routes.

## Persistence

- **PostgreSQL 16** in prod and local dev; **H2** in tests
  (`src/test/resources/application.yml`).
- **Flyway** owns the schema — migrations are `V{n}__*.sql` under
  `src/main/resources/db/migration/`. Migrations are immutable once
  merged; every schema change is a new `V{n+1}__*.sql`.
- JPA `spring.jpa.hibernate.ddl-auto=validate` (so a missing migration
  breaks boot in tests).
- `open-in-view=false` — every read that needs a JPA relationship must run
  inside a `@Transactional` service method.

### Entities (17)

| Entity | Table | Key fields |
| ------ | ----- | ---------- |
| `User` | `users` | `email` (unique), `password_hash`, `google_subject`, `default_mail_account_id`, `enabled`, `email_verified_at`, `password_set` |
| `MailAccount` | `mail_accounts` | FK `user_id`, `host`, `port`, `username`, `password_ciphertext`, `auth_type` (PASSWORD/XOAUTH2), `last_seen_uid`, `last_seen_sent_uid`, `signature` |
| `EmailThread` | `email_threads` | FK `owner_id`, `subject`, `root_message_id`, `conversation_key`, `message_count`, `unread` |
| `Message` | `messages` | FK `thread_id`, `message_id_header` (unique), FK `sender_id`, `body_plain`, `body_html`, `sent_at`, `outbound` |
| `Participant` | `participants` | `email` (unique), `display_name` |
| `MessageRecipient` | `message_recipients` | FK `message_id`, FK `participant_id`, `recipient_type` |
| `Attachment` | `attachments` | FK `message_id`, `filename`, `mime_type`, `size_bytes`, `blob_ref` (currently always NULL) |
| `Subscription` | `subscriptions` | unique FK `user_id`, `plan`, `status`, `stripe_*`, `billing_period`, `trial_ends_at`, `cancellation_reason` |
| `PlanChangeEvent` | `plan_change_events` | FK `subscription_id`, `from_plan`, `to_plan`, `occurred_at` |
| `PasswordResetToken` | `password_reset_tokens` | FK `user_id`, `token_hash` (unique), `expires_at`, `used_at` |
| `EmailVerificationToken` | `email_verifications` | FK `user_id`, `token_hash` (unique), `expires_at`, `used_at` |
| `AuthEvent` | `auth_events` | FK `user_id` (nullable), `email`, `event_type`, `ip_address` |
| `DigestEmailPreference` | `digest_email_preferences` | unique FK `user_id`, `opt_out_token` (unique), `opted_out` |
| `SavedSearch` | `saved_searches` | FK `owner_id`, `name`, `query`, `sender_email`, `since_preset`, `require_unread`, `require_attachments`, `last_viewed_at` |
| `Team` | `teams` | unique FK `owner_user_id` |
| `TeamMember` | `team_members` | FK `team_id`, FK `user_id`, `role` |
| `TeamInvite` | `team_invites` | FK `team_id`, FK `invited_by_user_id`, `invitee_email`, `token_hash` (unique), `expires_at`, `accepted_at`, `revoked_at` |

## Authentication

Two chains, both defined in `com.emailmessenger.auth.SecurityConfig` (form
chain) and `com.emailmessenger.web.api.ApiSecurityConfig` (bearer chain,
`@Order(1)` so it wins for `/api/v1/**`).

- **Form chain (default order, matches everything else):**
  `usernameParameter=email`, `passwordParameter=password`, `loginPage=/login`,
  `successHandler=PlanCheckoutSuccessHandler`,
  `failureHandler=LoginFailureHandler`, remember-me via `JdbcTokenRepositoryImpl`
  (30-day cookie), CSRF on (except for `/h2-console/**` and `/billing/webhook`),
  H2 console framing allowed.
- **Google OIDC** wires in only when `auth.google.client-id` +
  `.client-secret` are non-blank; provisioning goes through
  `OAuth2ProvisioningService` and stamps `google_subject` +
  `email_verified_at`.
- **API chain (`/api/v1/**`, `@Order(1)`):** stateless (`SessionCreationPolicy.STATELESS`),
  CSRF disabled, `ApiTokenAuthenticationFilter` extracts
  `Authorization: Bearer <token>`, looks up the SHA-256 hash in
  `api_tokens`, sets the authenticated principal to the same
  `UserDetails` produced by `DbUserDetailsService`. On failure, the JSON
  error entry point returns `{"error":"unauthorized"}` with `401`.
- **Login throttling** (`LoginThrottleFilter`) still runs before the form
  login filter, counting `LOGIN_FAILURE` rows per email/IP in a sliding
  window. The API login re-uses `LoginThrottleService` directly so both
  surfaces share limits.

## Email import + delivery

- **Polling** (`MailboxPollingService`, `@Scheduled`, feature-flagged by
  `mailbox.polling.enabled`) drives IMAP fetch + sent-folder sync,
  keeping the per-account `last_seen_uid` / `last_seen_sent_uid` cursors
  updated.
- **IDLE** (`ImapIdleService`, `mailbox.idle.enabled`) opens an IMAP IDLE
  connection per paid-tier account and publishes `NewMailEvent` when the
  server pushes new UIDs.
- **`EmailImportService`** parses `ParsedEmail`s into
  `EmailThread`/`Message`/`Attachment`/`Participant` rows, threading by
  `Message-ID` / `In-Reply-To` / `References` and grouping into
  conversation-key chats (V32 migration).
- **SSE** at `/chats/events` (`ChatEventStream`) fans `NewMailEvent`s out
  to any open browser tab. The mobile client will read a parallel stream
  at `/api/v1/events` (planned; not yet implemented).
- **Outbound** — `service.ReplyService` builds and sends via
  `JavaMailSender`; `OutboundMessageService` records the sent copy as a
  local `Message(outbound=true)` so the chat updates immediately.

## Billing

- **Stripe** (`billing.stripe.secret-key`, `.webhook-secret`,
  `.success-url`, `.cancel-url`, `.portal-return-url`, price IDs).
- **`BillingService`** orchestrates checkout / portal / cancel-reason
  capture and applies Stripe webhook events to `subscriptions`.
- **`PlanLimitService`** resolves the effective `Plan` from the active
  `Subscription` and enforces plan caps via `PlanLimitExceededException`,
  which the global exception handler turns into a flash-message + redirect
  on the web side (and a `403 {"error":"plan_limit"}` on the API side).

## Scheduled jobs

All are `@Scheduled` and gated by their own feature flag (default off in
prod unless the env var is set):

- `activation.enabled` — day-1 "connect a mailbox" nudge
- `reengagement.enabled` — 7-day-inactivity nudge with unread threads
- `digest.enabled` — weekly digest email
- `trial-end.enabled` — last-mile trial-end email
- `admin.weekly-digest.enabled` — operator's MRR/ARR/churn digest

## Configuration

Base file: `src/main/resources/application.yml`. Local overrides in
`application-local.yml` (profile `local`). Everything else comes from env
vars; there is no `application-prod.yml`.

Relevant env vars (see `application.yml` for the full list):

- Database: `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`
- Mail: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASS`
- Google OIDC: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- Stripe: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`
- Mailbox crypto: `MAILBOX_ENCRYPTION_PASSWORD`, `MAILBOX_ENCRYPTION_SALT`
- Feature flags: `MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`
- Marketing: `MARKETING_BASE_URL`, `MARKETING_LANDING_VIDEO_PROVIDER`,
  `MARKETING_LANDING_VIDEO_ID`, `MARKETING_LEGAL_PRIVACY|TERMS|REFUND`

## Related repositories

- **Mobile client:** `jacob-bensen/conexus-mail-mobile`
  - Consumes `docs/API.md` (this repo) as its source of truth.
- **This repo:** the backend + the website.

## Conventions

- Constructor injection only; no field `@Autowired`.
- Records for DTOs and view models; entities are classes.
- Package-private by default.
- Tests mirror the package of the class under test.
- No backwards-compatibility shims while pre-1.0 — change call sites.
- No comments that restate code — only non-obvious *why*.
