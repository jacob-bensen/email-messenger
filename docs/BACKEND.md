# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail", package `com.emailmessenger`).
Reverse-engineered 2026-10-30; items marked TBD were not verified in code.

> Stack note: persistence is **Spring Data JPA + Hibernate (`ddl-auto: validate`)**,
> not jOOQ. Flyway owns the schema (`src/main/resources/db/migration`, 35 files).
> PostgreSQL in prod/local, H2 in tests.

## Modules (packages)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: `/chats` workspace (`ConversationController`), mailboxes, Gmail connect, saved searches, pricing/landing/legal, PWA shell, SEO, SSE (`ChatEventsController`) |
| `auth` | Spring Security config, form login + Google OIDC, registration, email verification, password reset, account settings, login throttling/audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), billing banners, trial conversion |
| `email` | IMAP/SMTP: `ImapClient`, IDLE (`ImapIdleService`), `MailboxPollingService`, `EmailImportService`, `MimeMessageParser`, Gmail OAuth, credential encryption |
| `service` | Conversation logic: `ConversationService`, `ConversationListService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, weekly digests, activation nudges, win-back, admin revenue/funnel metrics |

## Schema overview (tables)

users, persistent_logins, auth_events, email_verifications, password_reset_tokens,
subscriptions, plan_change_events, mail_accounts, participants, email_threads,
messages, message_recipients, attachments, thread_notes, saved_searches,
digest_email_preferences, teams, team_members, team_invites (+ later column
additions through V35). Conversations are keyed by a participant-set
`conversationKey` (see `ConversationKeyService`).

## Auth model

- Session cookie (`JSESSIONID`) via form login (`/login`, params `email`/`password`)
  or Google OIDC; persistent remember-me (30 days, DB token store).
- **CSRF is enabled** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the `permitAll` list in `SecurityConfig` requires authentication.
- **No token / API-key auth exists.** A mobile client therefore cannot authenticate
  today except by scraping the web form. See `INTERNAL_TODO.md` #1.

## Web vs REST

There is **no REST API**. The only non-HTML responses are:
`POST /chats/refresh` (JSON `{"imported": n}`), `GET /chats/events` (SSE),
`/manifest.webmanifest`, `/sw.js`, icons, `robots.txt`, `sitemap.xml`,
`POST /billing/webhook` (Stripe). All business logic lives in services, so a future
`/api/v1` layer should be a thin controller set over the same services
(`ConversationListService`, `ConversationService`, `ReplyService`,
`MailAccountService`, …) — never copy logic out of `ConversationController`.
