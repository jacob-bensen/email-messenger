# Backend architecture

Audited 2026-11-10 from the code as it stands. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (not jOOQ — no jOOQ codegen exists),
PostgreSQL (H2 in tests), Flyway (`src/main/resources/db/migration`, V1–V35), Thymeleaf,
Spring Security (form login, Google OIDC, remember-me), Stripe, Jakarta Mail (IMAP/SMTP), jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation building: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, bubble/run view records |
| `email` | Mailbox integration: `MailboxPollingService`, `ImapIdleService`, `JakartaImapClient`, `MimeMessageParser`, `EmailImportService`, Gmail OAuth |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, password reset, email verification, account pages, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService` (Free/Pro/Business caps) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |

## Data model (Flyway tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts` (auth_type IMAP/OAuth, polling policy,
last-seen UID cursors, signature), `email_threads` (owner, `conversation_key`, unread),
`messages` (+ outbound flag), `message_recipients`, `participants`, `attachments` (`blob_ref` always null today),
`saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`,
`auth_events`, `teams`, `team_members`, `team_invites`.
A *conversation* in the UI = all threads sharing a participant-set `conversation_key`.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 days, persistent token repo) via form login
(`POST /login`, params `email`, `password`) or Google OIDC (`/auth/google/start`). **CSRF is enabled**
for everything except `/billing/webhook` and `/h2-console/**`. Everything not in the permit-list in
`SecurityConfig` requires authentication. There is **no token (Bearer/JWT) auth**.

## Web vs REST
The app is server-rendered. The only JSON/SSE endpoints are `POST /chats/refresh`, `GET /chats/events`
and the Stripe webhook. **No REST API for a mobile client exists yet** — see `API.md`.
Controllers delegate to services (`ChatService`, `MailboxPollingService`, `OutboundMessageService`…);
a future `/api/v1` layer must reuse these services, not the controllers.

## Commands
`./mvnw clean package`, `./mvnw test`, `./mvnw test -Dtest=Class#method`. See `CLAUDE.md`.
