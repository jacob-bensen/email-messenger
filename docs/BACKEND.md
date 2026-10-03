# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Source of truth for persistence,
auth, billing and all business rules. Serves both the Thymeleaf website and
(planned) a REST API for the mobile client (`conexus-mail-mobile`).

> Reality check (2026-10-03 audit): persistence is **Spring Data JPA** (not jOOQ)
> and **no REST/JSON API exists yet** — see `docs/API.md`.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `ChatEventStream` SSE) |
| `service/` | Conversation model: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `email/` | Mailbox integration: IMAP (`JakartaImapClient`, IDLE), Gmail OAuth, `EmailImportService`, `MimeMessageParser`, polling (`MailboxPollingService`/`Scheduler`), `CredentialEncryptor` |
| `domain/` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `repository/` | Spring Data repositories |
| `auth/` | Spring Security config, form + Google OIDC login, registration, password reset, email verification, login throttling, account settings |
| `billing/` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` | Teams/invites, weekly digests, lifecycle email schedulers, admin metrics |

## Persistence

PostgreSQL 16 (prod/local), H2 (tests). Flyway migrations `V1`..`V35` in
`src/main/resources/db/migration` (immutable once merged). Main tables: `users`,
`persistent_logins`, `mail_accounts`, `email_threads` (carries `conversation_key`),
`messages` (+ outbound flag), `message_recipients`, `participants`, `attachments`,
`subscriptions`, `plan_change_events`, `teams`, `team_members`, `team_invites`,
`saved_searches`, `digest_email_preferences`, `password_reset_tokens`,
`email_verifications`, `auth_events`.

## Auth model

Session cookie (Spring Security form login + Google OIDC), remember-me
(`persistent_logins`), CSRF enabled (ignored only for `/h2-console/**` and
`/billing/webhook`). Public paths are whitelisted in `SecurityConfig`; everything
else requires authentication. **There is no token (Bearer/JWT) auth yet** — needed
before a mobile client can call the server (see `INTERNAL_TODO.md`).

## Web vs API

Controllers are thin and delegate to services (`ChatService`, `MailAccountService`,
`BillingService`, ...). The only JSON-returning web endpoint is
`POST /chats/refresh`; the SSE stream is `GET /chats/events`. A future REST API
must reuse these same services — do not fork logic into controllers.

## Background work

`@Scheduled` mailbox polling (flag-guarded), IMAP IDLE for paid plans
(`mailbox.idle.enabled`), weekly digests, activation/re-engagement/trial-end emails.
