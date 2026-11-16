# ConexusMail backend — architecture

Spring Boot 3 / Java 21 app that imports email over IMAP (or Gmail OAuth)
and serves it as texting-style conversations. It is the authoritative
owner of all state and rules, serves the website (`docs/WEB.md`), and will
serve the mobile API (`docs/API.md`).

Last audited against the code: 2026-11-16.

## Stack (actual)

- Java 21, Spring Boot 3, Maven wrapper (`./mvnw`).
- Persistence: **Spring Data JPA / Hibernate** (not jOOQ — there is no jOOQ
  or codegen step). PostgreSQL 16 in prod/local, H2 in tests.
- Flyway migrations `src/main/resources/db/migration/V1..V35`. Immutable
  once merged; add a new `V{n}__*.sql`.
- Thymeleaf views, Spring Security (form login, remember-me, optional
  Google OIDC), Jakarta Mail (IMAP + SMTP), Stripe Java SDK.
- Tests: JUnit 5, Spring Boot Test, GreenMail, Testcontainers.

## Packages (`com.emailmessenger`)

| Package | Responsibility |
|---|---|
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `Plan`, `SavedSearch`, `Team*`, `AuthEvent`, tokens, `DigestEmailPreference`, `PlanChangeEvent` |
| `repository` | Spring Data repositories, one per aggregate |
| `email` | Mailbox integration: `MailAccountService` (connect/remove), `ImapClient`/`JakartaImapClient`, `GmailOAuthClient`, `CredentialEncryptor`, `EmailImportService` + `MimeMessageParser` (RFC 822 → threads via Message-ID/In-Reply-To/References), `MailboxPollingService`/`Scheduler` (polling, sent-folder sync, `NewMailEvent`), `ImapIdleService` (IDLE for paid plans), `OwnerAddressService`, `ConversationKeyBackfill` |
| `service` | Conversation building: `ConversationKeyService`, `ConversationLabels`, `IMTransformService`, `EmailBodyCleaner` (quote/signature stripping), `ReplyService` (SMTP send), `ConversationService`, `DemoConversationService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `web` | Controllers + web-scoped services: `ConversationController`, `ChatService` (one conversation's merged timeline, mark-read), `ConversationListService` (chat list, filters, counts), `OutboundMessageService` (records sent mail locally), `ChatEventStream` (SSE), mailboxes, saved searches, onboarding, PWA, SEO, legal |
| `auth` | `SecurityConfig`, users/registration, login throttle + audit, password reset, email verification, Google OIDC provisioning, account settings |
| `billing` | Stripe checkout/portal/webhooks, `PlanLimits` + `PlanLimitService`, trial conversion, banners |
| `team` | Teams and invites |
| `admin` | Operator metrics (`/admin/revenue`), win-back outreach, weekly operator digest |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |

## Core data model

- `users` 1—* `mail_accounts` (IMAP password or Gmail OAuth, encrypted
  credentials, polling cursors `last_seen_uid` / `last_seen_sent_uid`,
  backoff, signature).
- `email_threads` (owner, subject, root Message-ID, unread flag,
  **`conversation_key`**) 1—* `messages` (sender participant, bodies,
  sent-at, `outbound` flag) 1—* `message_recipients`, `attachments`
  (metadata only; `blob_ref` is always null).
- `participants` deduped by normalized email.
- A **conversation** is not a table: it is every thread of one owner sharing
  a `conversation_key` (`API.md` §4.1), merged into one timeline.
- Billing: `subscriptions` (plan, Stripe ids, status, period, cancellation
  and win-back tracking), `plan_change_events`.
- Others: `saved_searches`, `teams`/`team_members`/`team_invites`,
  `auth_events`, `persistent_logins`, token tables, `digest_email_preferences`.

## Auth model

One `SecurityFilterChain` (`auth.SecurityConfig`): form login at `/login`
(`email`/`password`), BCrypt, remember-me (30 days, JDBC tokens),
`LoginThrottleFilter` before the auth filter, CSRF on (exempt:
`/billing/webhook`, `/h2-console/**`), Google OIDC only when Google client
credentials are configured. Public routes are listed explicitly; everything
else requires a session. There is no token auth — see `API.md` §5.

## Background jobs (`@Scheduled`, flag-gated in `application.yml`)

Mailbox polling (`mailbox.polling.enabled`), IMAP IDLE
(`mailbox.idle.enabled`), SSE keepalive, weekly digest, activation nudges,
re-engagement, trial-end conversion, operator weekly digest.

## Web pages vs. the (future) REST API

Today only the website exists. Most rules live in services that a REST layer
can reuse (`ChatService`, `ConversationListService`, `MailAccountService`,
`PlanLimitService`, `ConversationKeyService`). **Exception:** the send flow
(recipient parsing, sender-account choice, signature append, default
subject, reply-vs-new decision) lives inside `ConversationController` and
must move into a service before an API endpoint sends mail — tracked in
`INTERNAL_TODO.md`.
