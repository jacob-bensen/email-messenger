# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-09. Unknowns are marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3.5.x, Spring MVC + Thymeleaf, Spring Security (form login,
remember-me, optional Google OIDC), **Spring Data JPA/Hibernate** (`ddl-auto: validate`),
Flyway (V1..V35), PostgreSQL (H2 in tests), Jakarta Mail (IMAP/SMTP), Stripe Java SDK, jsoup.
There is **no jOOQ** — the routine's "jOOQ" assumption does not apply here; persistence is JPA
entities in `domain/` + Spring Data repositories.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PwaController`, `SeoController`, `LegalController`, `PricingController`, `ChatEventsController` SSE), chat/list services, `GlobalExceptionHandler`, model advices |
| `service` | Conversation/IM transform: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | Mailbox integration: `EmailImportService`, `MimeMessageParser`, `ImapClient`/`JakartaImapClient`, IDLE (`ImapIdleService`), `MailboxPollingService` + scheduler, `GmailOAuthClient`, `CredentialEncryptor`, `MailAccountService` |
| `auth` | Security config, registration/login, password reset, email verification, Google OIDC, account pages, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial-end & banner services, cancellation reason |
| `admin` | `/admin/**` operator dashboards (allowlist `admin.emails`; non-admin gets 404) |
| `team`, `digest`, `activation`, `reengagement` | Teams/invites, weekly digest, lifecycle emails (cron schedulers off unless `*.enabled=true`) |
| `domain` | JPA entities: User, Subscription, MailAccount, EmailThread, Message, MessageRecipient, Participant, Attachment, SavedSearch, Team*, auth/token entities, PlanChangeEvent |
| `repository` | Spring Data repositories |

## DB schema overview (Flyway, immutable once merged)
Tables: `users`, `subscriptions` (plan, billing_period, cancellation_reason, win-back/trial tracking),
`mail_accounts` (auth_type password|oauth, encrypted credentials, last_seen_uid / sent uid cursor, signature),
`participants` (deduped by normalized email), `email_threads` (owner, `conversation_key`, unread),
`messages` (outbound flag), `message_recipients`, `attachments` (`blob_ref` always null today),
`saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`,
`auth_events`, `persistent_logins`, `teams`, `team_members`, `team_invites`, `plan_change_events`.
Next migration number: **V36**.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 d, JDBC tokens), form login at `POST /login`
(`email`, `password`), CSRF **enabled** (exempt: `/h2-console/**`, `/billing/webhook`).
Public paths are an allowlist in `auth/SecurityConfig`; everything else requires authentication.
Google sign-in only when `GOOGLE_CLIENT_ID/SECRET` are set. **No token/bearer auth exists** — a
mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
The app is entirely server-rendered. Controllers return view names; the only JSON/non-HTML
handlers are `POST /chats/refresh` (JSON), `GET /chats/events` (SSE) and `POST /billing/webhook`.
No `@RestController` exists. See `API.md` and `WEB.md`.

## Key behaviours
- Conversations are keyed by participant-set `conversationKey`; threads merge into one timeline.
- Import: polling (+IMAP IDLE for paid tiers) → `EmailImportService` → `NewMailEvent` → SSE.
- Plan limits are enforced server-side in `PlanLimitService`.
