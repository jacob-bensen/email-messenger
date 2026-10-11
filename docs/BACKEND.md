# BACKEND.md — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). It serves **server-rendered
Thymeleaf pages** (the website *and* the web app) from the same process that owns
persistence and business logic. As of 2026-10-11 there is **no JSON REST API for
the mobile client** — see `API.md` and `INTERNAL_TODO.md`.

> Classification evidence: `pom.xml` + Spring annotations + Flyway migrations
> (35 files in `src/main/resources/db/migration`). Persistence is **Spring Data
> JPA/Hibernate (`ddl-auto: validate`), not jOOQ** — the routine's jOOQ assumption
> does not hold here; there is no codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `PricingController`, `SavedSearchController`, `PwaController`, `SeoController`, `LegalController`), `ChatEventsController` + `ChatEventStream` (SSE), `ChatService`, `ConversationListService`, `OutboundMessageService`, `GlobalExceptionHandler` |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`, `ReplyService`), view records (`ChatConversation`, `SenderBubbleRun`, `BubbleMessage`, `ConversationListItem`, `ChatMember`) |
| `email` | Mailbox integration: `ImapClient`/`JakartaImapClient`, IDLE (`ImapIdleService`), polling (`MailboxPollingService`/`Scheduler`, `PollingPolicy`), `MimeMessageParser`, `EmailImportService`, Gmail OAuth (`GmailOAuthClient`), `CredentialEncryptor`, `MailAccountService` |
| `auth` | Form login + Google OIDC, registration, email verification, password reset, login throttle, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team` | Team invites/membership |
| `digest`, `activation`, `reengagement`, `admin` | Schedulers/emails and admin metrics (`/admin/revenue`) |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema overview (Flyway V1..V35, immutable once merged)
Core: `users`, `mail_accounts` (IMAP password or Gmail OAuth, encrypted creds,
polling state), `email_threads` (has `conversation_key` = participant-set key),
`messages`, `message_recipients`, `participants`, `attachments` (`blob_ref` always
null today — bytes are not stored), `thread_notes`.
Product: `subscriptions`, `plan_change_events`, `saved_searches`, `teams`,
`team_members`, `team_invites`, `digest_email_preferences`.
Security: `auth_events`, `password_reset_tokens`, `email_verifications`,
`persistent_logins` (remember-me). Plans: `FREE`, `PRO`, `BUSINESS`.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,
`password`) or Google OIDC (only when `GOOGLE_CLIENT_ID` is set); remember-me
(30 days). CSRF **enabled** everywhere except `/h2-console/**` and
`/billing/webhook`. Everything not on the permit-all list (see `WEB.md`) needs an
authenticated session. No bearer/token auth exists → a mobile client cannot
authenticate cleanly yet.

## Web vs. API
There is no API layer. Controllers resolve the user (`UserService.requireByEmail`)
and delegate to services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, `BillingService`…). A future
`/api/v1` controller layer must reuse these same services — do not copy logic.

## Realtime
`GET /chats/events` is an SSE stream (`NewMailEvent` published by
`MailboxPollingService`); IMAP IDLE for paid tiers behind `mailbox.idle.enabled`.
Web Push is not implemented (roadmap in `CLAUDE.md`).
