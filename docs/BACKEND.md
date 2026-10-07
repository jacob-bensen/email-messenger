# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-07. Anything not verifiable in code is marked **TBD**.

## Stack (actual)

Java 21, Spring Boot 3 (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client, actuator),
**Spring Data JPA / Hibernate** (NOT jOOQ — there is no jOOQ codegen), Flyway (35 migrations, `V1`..`V35`),
PostgreSQL 16 (prod/local) / H2 (tests), Jakarta Mail (IMAP/SMTP), Stripe Java SDK, jsoup, GreenMail + Testcontainers (tests).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail connect, saved searches, pricing/landing/legal, PWA, SEO, SSE (`ChatEventsController`, `ChatEventStream`) |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `service` | Conversation read model: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `email` | Mailbox integration: `ImapClient`/`JakartaImapClient`, `MailboxPollingService`+scheduler, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, Gmail OAuth, `CredentialEncryptor` |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits`/`PlanLimitService`, trial-end conversion |
| `team` | Teams and invites |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |
| `admin` | Operator revenue/retention metrics (`/admin/revenue`) |

## Domain / schema overview

Tables (entities): `users`, `subscriptions`, `plan_change_events`, `teams`, `team_members`, `team_invites`,
`mail_accounts`, `email_threads`, `messages`, `message_recipients`, `participants`, `attachments`,
`saved_searches`, `digest_email_preferences`, `auth_events`, `email_verifications`, `password_reset_tokens`
(+ Spring Security `persistent_logins` for remember-me). A *conversation* in the UI is keyed by
`conversationKey` (participant-set hash, `ConversationKeyService`) and can merge several `email_threads`.
Attachments: `blob_ref` is always null — bytes are not stored (roadmap item in `CLAUDE.md`).

## Auth model

- Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token repo). CSRF **enabled**
  (exempt: `/h2-console/**`, `/billing/webhook`).
- Form login at `POST /login` (params `email`, `password`); Google OIDC when `GOOGLE_CLIENT_ID` is set.
- Everything not in the permit-all list in `SecurityConfig` requires authentication.
- **No token/bearer/API-key auth exists.** A mobile client cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs REST

The backend is a server-rendered Thymeleaf app. Controllers return view names; the only non-HTML
responses are `POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook`,
robots/sitemap/PWA assets. **There is no `/api/**` REST layer.** Business logic lives in services
(`ConversationService`, `ReplyService`, `MailAccountService`, `BillingService`, …), which is the
layer a future REST controller must reuse. Known exception: `ConversationController` (504 lines)
holds mailbox-scoping/selection logic (`selectMailbox`, `scopeAddress`, filter parsing) that would
need extracting into a service before an API can share it.

## Background jobs

`MailboxPollingScheduler` (5 min, `mailbox.polling.interval-ms`), `ImapIdleService` reconcile (60 s,
paid plans), SSE keepalive (25 s), weekly digest, activation/re-engagement/trial-end daily crons,
admin weekly digest.
