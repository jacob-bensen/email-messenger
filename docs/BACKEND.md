# BACKEND — architecture

Reverse-engineered from the code on 2026-10-01. Anything not verifiable in code is marked TBD.

## Role
Spring Boot 3 / Java 21 app ("ConexusMail", also called MailIM in `master/PLAN.md`). It owns persistence,
all business rules, and the server-rendered website (Thymeleaf). It imports email over IMAP and presents
it as a texting-style `/chats` workspace. **There is no REST/JSON API for a mobile client yet** — see
`docs/API.md`.

## Stack (actual)
- Java 21, Spring Boot 3, Maven wrapper (`./mvnw`).
- **JPA/Hibernate + Spring Data** (`ddl-auto: validate`) — NOT jOOQ (the routine template assumes jOOQ).
- PostgreSQL (prod/local), H2 (tests), **Flyway** migrations `V1`..`V35` in `src/main/resources/db/migration`.
- Thymeleaf templates in `src/main/resources/templates`.
- Stripe (billing), Jakarta Mail (IMAP/SMTP), Gmail OAuth, Google OIDC sign-in.

## Packages (`com.emailmessenger`)
| Package | Contents |
|---|---|
| `domain` | JPA entities: `User`, `Subscription`, `Plan`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `SavedSearch`, `Team*`, token/audit entities |
| `repository` | Spring Data repositories |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation key (`ConversationKeyService`), view records (`ChatConversation`, `ConversationListItem`), `ReplyService` |
| `email` | IMAP import/polling/IDLE (`EmailImportService`, `MailboxPollingService`, `ImapIdleService`), Gmail OAuth, credential encryption |
| `web` | Thymeleaf controllers + web services (`ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream`) |
| `auth` | `SecurityConfig`, registration/login/reset/verify, Google OIDC, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits`, `PlanLimitService` |
| `admin` | `/admin/**` revenue/retention metrics (allow-list `admin.emails`) |
| `team`, `digest`, `activation` | invites, weekly digests, activation emails |

## Data model (tables, from migrations)
`users`, `persistent_logins`, `subscriptions`, `mail_accounts`, `participants`, `email_threads`
(`conversation_key`, `unread`, `owner`), `messages` (`outbound`, `message_id_header`, `in_reply_to`),
`message_recipients`, `attachments` (`blob_ref` always null — bytes are not stored), `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`,
`teams`, `team_members`, `team_invites`, `plan_change_events`. `thread_notes` was dropped in V33.

A **conversation** = all threads of one owner sharing a `conversation_key` (participant-set hash).

## Plans
`Plan {FREE, PRO, BUSINESS}`; caps in `billing/PlanLimits` (Free 3 mailboxes / 500 threads; Pro 5 / unlimited;
Business unlimited). Note `master/PLAN.md` still describes an older price/limit model — drift, see TODO.

## Auth model
Session-cookie form login (`/login`, field `email`/`password`), remember-me (30 d, JDBC token repo), optional
Google OIDC (only when `GOOGLE_CLIENT_ID` set), BCrypt, login throttle filter. CSRF is **on** except
`/h2-console/**` and `/billing/webhook`. Everything not whitelisted in `SecurityConfig` requires auth.
No bearer/JWT/API-token mechanism exists (TBD design for mobile).

## Web vs API
All controllers are `@Controller` returning view names (plus a few `@ResponseBody`/SSE/webhook endpoints).
Business logic lives in services (`ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, `BillingService`, ...), which a future REST layer should reuse.
Some logic currently lives in `ConversationController` (private helpers: `selectMailbox`, `scopeAddress`,
`accountColorMap`, `fallbackAccount`, `parseRecipients`, `parseFilter`) — must be extracted before an API
can share it (see `INTERNAL_TODO.md`).

## Background jobs
`MailboxPollingScheduler` (flag `mailbox.polling.enabled`), `ImapIdleService` (`mailbox.idle.enabled`),
weekly/activation/trial-end/admin-digest schedulers (each off unless its `*.enabled` flag is true).

## Config
Env: `JDBC_DATABASE_URL/USERNAME/PASSWORD`, `MAIL_*`, `GOOGLE_CLIENT_ID/SECRET`, `STRIPE_*`,
`MAILBOX_ENCRYPTION_PASSWORD/SALT`. Profiles: `local`, `prod`. See `DEPLOY.md`.

## Other planning docs
`master/PLAN.md`, `master/BACKLOG.md`, `master/MASTER_ACTIONS.md`, `master/CHANGELOG.md` (the pre-existing
operator-routine docs, ~500 KB changelog). Root `CLAUDE.md` holds the roadmap checklist.
