# Backend architecture

Reverse-engineered from the code on 2026-10-22. Anything not verified is marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — there is no jOOQ in `pom.xml`, so
no codegen step), PostgreSQL 16 (H2 in tests), Flyway (`V1`–`V35`, immutable), Thymeleaf,
Spring Security (form login + Google OIDC + persistent remember-me), Stripe, Jakarta Mail
(IMAP/SMTP), GreenMail + Testcontainers in tests.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/legal/PWA/SEO). Also `ChatEventStream` (SSE). |
| `service` | Chat/conversation view models and logic: `ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`. |
| `email` | Mailbox integration: `MailAccountService`, `MailboxPollingService`/`Scheduler`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor`. |
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Plan`, `Team*`, `SavedSearch`, auth-token entities. |
| `repository` | Spring Data repositories. |
| `auth` | Security config, registration/login, Google OIDC, password reset, email verification, account settings. |
| `billing` | Stripe checkout/portal/webhook gateways, plan limits (`PlanLimitService`, `PlanLimitKind`). |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails (schedulers off unless `*.enabled`), admin metrics. |

## Core model
`Message` -> `EmailThread` (Message-ID / In-Reply-To / References) -> grouped into a
*conversation* by `EmailThread.conversationKey` (participant-set key, V32). A `User` owns
`MailAccount`s (IMAP password or Gmail OAuth; credentials encrypted). Mail arrives by
polling (`mailbox.polling.enabled`) and IMAP IDLE for paid plans (`mailbox.idle.enabled`),
publishing `NewMailEvent` -> `ChatEventStream` (SSE).

## Auth model
Browser sessions only: cookie `JSESSIONID`, CSRF enabled (exempt: `/h2-console/**`,
`/billing/webhook`), remember-me cookie (30 days, DB-backed). Google sign-in only when
`GOOGLE_CLIENT_ID` is set. Everything except an allowlist in `SecurityConfig` requires
authentication. **There is no token/bearer auth, so a mobile client cannot authenticate yet.**

## Web vs. REST
There is no REST API for clients. Controllers return Thymeleaf views and redirects; the only
JSON/machine endpoints are `POST /chats/refresh`, `GET /chats/events` (SSE) and
`POST /billing/webhook`. See `docs/API.md`, `docs/WEB.md`.

## Known structural risk
`ConversationController` holds orchestration logic (mailbox scoping `selectMailbox`/
`scopeAddress`, signature appending, send-as-reply vs. new-thread). A future API must not
copy it — extract into services first (see `INTERNAL_TODO.md`).
