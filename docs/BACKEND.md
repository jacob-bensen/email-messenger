# BACKEND.md — architecture

Reverse-engineered 2026-10-23 from the code. Unknowns are marked TBD.

## Stack

Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (not jOOQ — there is no
codegen step), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security,
Stripe, Jakarta Mail (IMAP/SMTP). Maven wrapper (`./mvnw`).
`spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`.

## Package layout (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers for the chats workspace, mailboxes, saved searches, pricing/landing, legal, PWA assets, SSE stream (`/chats/events`) |
| `auth` | Form + Google OIDC login, registration, password reset, email verification, account pages, `SecurityConfig`, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), banners |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE for paid plans, Gmail OAuth, credential encryption |
| `service` | Conversation building, IM transform, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, operator metrics (`/admin/**`, allowlist via `admin.emails`) |

## Schema overview (Flyway `V1`..`V35`, immutable once merged)

`users`, `persistent_logins`, `subscriptions`, `plan_change_events`,
`mail_accounts` (IMAP password or Gmail OAuth; encrypted creds; last-seen
UID cursors incl. sent folder), `participants`, `email_threads`
(owner, unread, `conversation_key` = participant-set key), `messages`
(outbound flag), `message_recipients`, `attachments` (`blob_ref` always
null today — bytes are not stored), `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`,
`auth_events`, `teams`, `team_members`, `team_invites`.
`thread_notes` was created in V25 and dropped in V33.

## Auth model

- Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token store).
- Form login at `POST /login` (`email`, `password`); optional Google OIDC when
  `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` are set.
- CSRF is **on** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not in the `permitAll` list in `SecurityConfig` requires authentication.
- There is **no token/bearer auth** and **no JSON API** for a mobile client (see API.md).

## Web vs REST

All user-facing behavior is server-rendered Thymeleaf. The only JSON-ish
endpoints are the Stripe webhook (`@RestController`) and the SSE stream.
Controllers delegate to services (`ChatService`, `ConversationListService`,
`ReplyService`, `MailAccountService`, `BillingService`, …); a future mobile API
must reuse those services rather than the controllers.

## Background jobs

`MailboxPollingScheduler` (flag `mailbox.polling.enabled`, default on),
`ImapIdleService` (`mailbox.idle.enabled`, default on, paid plans),
plus flag-gated cron schedulers for digests, activation, re-engagement,
trial-end, and the operator weekly digest.

## Config

Env vars: `JDBC_DATABASE_URL/USERNAME/PASSWORD`, `MAIL_*`, `GOOGLE_CLIENT_*`,
`STRIPE_*`, `MAILBOX_ENCRYPTION_PASSWORD/SALT`, `MAILBOX_POLLING_ENABLED`,
`MAILBOX_IDLE_ENABLED`. Profiles: `local`, `prod`.
