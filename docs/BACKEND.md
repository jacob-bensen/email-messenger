# Backend architecture

Status: reverse-engineered from the code on 2026-11-10. Anything not verified is marked **TBD**.

Spring Boot 3 / Java 21 / Maven monolith. It serves the public website *and* the authenticated web app
via Thymeleaf. **There is currently no JSON REST API** (see `docs/API.md`).

> Deviation from the generic stack assumptions: persistence is **Spring Data JPA/Hibernate**
> (`ddl-auto: validate`), **not jOOQ**. There is no jOOQ codegen step. Schema is owned by Flyway.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, email verification, password reset, login throttling, account settings (`SecurityConfig`) |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial/upgrade nudges |
| `email` | Mailbox connect (IMAP password + Gmail OAuth), IMAP client, polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), MIME parsing, import (`EmailImportService`), credential encryption |
| `service` | Conversation building: `ConversationKeyService` (participant-set key), `ChatConversation`/bubble runs, `EmailBodyCleaner` (quote/signature stripping), `IMTransformService`, `ReplyService` |
| `web` | Thymeleaf controllers (`ConversationController` = `/chats`), `ChatEventStream` (SSE), landing/pricing/legal/SEO/PWA, onboarding |
| `domain` | JPA entities |
| `repository` | Spring Data repositories |
| `team` | Teams + invites |
| `digest`, `reengagement`, `activation`, `admin` | Scheduled emails (feature-flagged) and admin metrics (`/admin/**`, allowlist `admin.emails`) |

## Data model (Flyway V1–V35)

Core: `users`, `persistent_logins` (remember-me), `subscriptions`, `plan_change_events`, `mail_accounts`
(encrypted credentials, `last_seen_uid`, `last_seen_sent_uid`, polling policy, `signature`),
`participants`, `email_threads` (owner, `conversation_key`, `unread`), `messages` (`outbound` flag,
`message_id_header` unique), `message_recipients`, `attachments` (`blob_ref` always null today),
`saved_searches`, `digest_email_preferences`, `teams`/`team_members`/`team_invites`,
`auth_events`, `email_verifications`, `password_reset_tokens`. `thread_notes` was dropped in V33.
Migrations are immutable; add a new `V{n}`.

A *conversation* (`/chats/{key}`) is a participant-set merge of one or more `email_threads` sharing a
`conversation_key`.

## Auth model

- Session cookie (`JSESSIONID`) + optional persistent remember-me (30 days, JDBC token repo).
- Form login at `POST /login` (`email`, `password`), Google OIDC when `GOOGLE_CLIENT_ID/SECRET` set.
- CSRF **enabled** for everything except `/h2-console/**` and `/billing/webhook`.
- Login throttling filter before the form-login filter.
- Everything not on the public allowlist in `SecurityConfig` requires authentication.
- **No bearer/token auth exists.** A mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs API

Controllers return Thymeleaf view names or redirects; they delegate to services (`ChatService`,
`ConversationListService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, …). The
only non-HTML responses: SSE `/chats/events`, `/billing/webhook`, PWA assets, robots/sitemap.
A future REST layer must reuse these services, not the controllers.

## Live delivery

`MailboxPollingService` imports new mail and publishes `NewMailEvent`; `ChatEventStream` fans out over
SSE (`GET /chats/events`); `ImapIdleService` holds IDLE connections for paid accounts.

## Commands

See `CLAUDE.md`. Build/test: `./mvnw test` (or `mvn -q -B verify`). Tests use H2.
