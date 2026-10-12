# Backend architecture

Spring Boot 3 / Java 21 / Maven monolith ("ConexusMail", repo `email-messenger`).
It serves the **server-rendered website** (Thymeleaf) and owns all persistence and
business logic. There is currently **no REST API for the mobile client** — see
`docs/API.md` and `INTERNAL_TODO.md`.

> Deviation from the generic stack assumption: persistence is **Spring Data JPA /
> Hibernate** (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen step.
> Flyway owns the schema.

## Package layout (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail OAuth connect, saved searches, pricing/landing/legal/SEO, PWA shell (manifest, `sw.js`, icons) |
| `auth` | Form login, registration, Google OIDC, password reset, email verification, account settings, login throttling, auth-event audit, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan caps (`PlanLimits`, `PlanLimitService`), trial/billing banners |
| `email` | IMAP polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), MIME parsing/import (`EmailImportService`), Gmail OAuth, credential encryption |
| `service` | Conversation logic: `ConversationService`, `ConversationListService`, `ChatService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `repository` | Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digest, lifecycle emails, operator metrics (`/admin/**`, allowlisted by `admin.emails`) |

## Data model (Flyway `V1`–`V35`, immutable once merged)

- `users` (+ acquisition source, Google subject, lifecycle tracking, mail prefs)
- `subscriptions`, `plan_change_events` — plan FREE / PRO / BUSINESS
- `mail_accounts` — IMAP password (encrypted) or Gmail OAuth; polling policy; `last_seen_uid`, `last_seen_sent_uid`; signature
- `email_threads` — owner, subject, `conversation_key` (participant-set hash), unread, message_count
- `messages` — `message_id_header` (unique), `in_reply_to`, bodies, `sent_at`, outbound flag
- `participants`, `message_recipients`, `attachments` (metadata only; `blob_ref` always null today)
- `saved_searches`, `digest_email_preferences`, `teams`/`team_members`/`team_invites`
- `password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins` (remember-me)

A *chat* is the set of threads sharing a `conversation_key`, merged into one
timeline per participant set and rendered as bubble runs.

## Auth model

- Browser session cookie (`JSESSIONID`) via form login (`email`/`password`), Google OIDC
  (only when `GOOGLE_CLIENT_ID` is set), 30-day persistent remember-me.
- BCrypt passwords; `LoginThrottleFilter` locks repeated failures; CSRF **enabled**
  (exempt: `/h2-console/**`, `/billing/webhook`).
- Everything except an explicit allowlist (see `SecurityConfig`) requires authentication.
- `/admin/**` additionally requires the email to be in `admin.emails` (404 otherwise).
- **No token/API-key auth exists.** A mobile client cannot authenticate today without
  scraping the form login + CSRF flow.

## Web vs API

All user-facing behavior is Thymeleaf MVC. Controllers are thin and delegate to the
service layer (`ChatService`, `ReplyService`, `MailAccountService`, `PlanLimitService`,
...). The only JSON/streaming endpoints are `POST /chats/refresh`, `GET /chats/events`
(SSE) and `POST /billing/webhook`. A future `/api/v1` must call the same services —
do not copy controller logic.

## Background work

`@Scheduled` jobs gated by `*.enabled` flags: mailbox polling (on by default), IMAP IDLE for
paid plans, weekly digest, reengagement, activation, trial-end, admin digest.

## Config

Env: `JDBC_DATABASE_URL/USERNAME/PASSWORD`, `MAIL_*`, `GOOGLE_CLIENT_ID/SECRET`,
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `MAILBOX_ENCRYPTION_PASSWORD/SALT`,
`MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`. Tests use H2.
