# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website plus background jobs. Persistence is **Spring Data JPA** (Hibernate,
`ddl-auto: validate`) over PostgreSQL 16, with Flyway migrations
(`src/main/resources/db/migration`, V1..V35). **There is no jOOQ** in this repo
(the generic routine assumed it; the real stack is JPA).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf MVC controllers (`ConversationController` = /chats), SSE (`ChatEventStream`), PWA/SEO, mailbox + saved-search controllers |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversations (`ConversationService`, `ConversationKeyService`), replies (`ReplyService`) |
| `email` | IMAP/Gmail import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IDLE (`ImapIdleService`), credential encryption |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, login throttle, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, `/admin/revenue` metrics |
| `domain`, `repository` | JPA entities, Spring Data repositories |

## Schema overview (tables)

users, subscriptions, plan_change_events, auth_events, email_verifications,
password_reset_tokens, persistent_logins, mail_accounts, email_threads,
messages, message_recipients, participants, attachments (`blob_ref` always
null today), saved_searches, thread_notes, teams, team_members, team_invites,
digest_email_preferences.

A conversation in /chats is keyed by `conversationKey` (participant-set hash,
`ConversationKeyService`), merging several email threads into one timeline.

## Auth model

- Browser **session cookie** (`JSESSIONID`) via form login (`/login`) or Google
  OIDC; remember-me cookie; **CSRF enabled** (ignored only for
  `/h2-console/**`, `/billing/webhook`).
- Everything except the public allow-list in `SecurityConfig` requires an
  authenticated session. **No token/bearer auth exists** — a mobile client
  cannot authenticate today (see `INTERNAL_TODO.md` #1).
- Mailbox credentials (IMAP password / Gmail refresh token) are encrypted at
  rest (`CredentialEncryptor`).

## Web vs REST

There is **no REST/JSON API**. The only non-HTML endpoints are SSE
`/chats/events`, PWA assets and SEO files. All business logic lives in
services, controllers are thin; the future `/api/v1` layer must call the same
services (`ConversationListService`, `ChatService`, `OutboundMessageService`,
`MailAccountService`).

## Runtime

Profiles: base (env `JDBC_DATABASE_*`), `local`, tests on H2. Schedulers:
mailbox polling, IMAP IDLE (paid tiers), digests, trial/win-back/reengagement.
