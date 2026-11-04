# BACKEND.md — architecture (reverse-engineered 2026-11-04)

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf UI
plus background jobs. **There is currently no JSON REST API** (see `API.md`).

> Deviation from the generic sync-routine assumption: persistence is
> **Spring Data JPA + Flyway**, not jOOQ. No codegen step exists.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/landing/legal/SEO/PWA), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (FREE 3 mailboxes/500 threads; PRO 5/unlimited; BUSINESS unlimited), banners, trial nudges |
| `email` | IMAP import (`MailboxPollingService`, `EmailImportService`, `MimeMessageParser`), IMAP IDLE (paid tiers), Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService`, view-model records |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team` | Team invites & membership |
| `admin` | Revenue/funnel/churn metrics, weekly digest to admins |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)
Core: `users`, `subscriptions`, `mail_accounts`, `email_threads`
(`conversation_key` = participant-set key, V32), `messages` (outbound flag V30),
`message_recipients`, `participants`, `attachments` (`blob_ref` always null —
bytes not stored), `saved_searches`, `teams`/`team_members`/`team_invites`,
`password_reset_tokens`, `email_verifications`, `auth_events`,
`plan_change_events`, `digest_email_preferences`, `persistent_logins`.
Migrations are immutable; add `V36+`.

## Auth model
Session cookie (form login or Google OIDC), remember-me (persistent_logins),
CSRF enabled (ignored only for `/billing/webhook`, `/h2-console/**`). Everything
not in the permitAll list in `SecurityConfig` requires authentication. No
token/bearer auth exists → **mobile cannot authenticate yet**.

## Web ↔ API relationship
All behavior is reached through Thymeleaf controllers that call services
(`ChatService`, `ConversationListService`, `ReplyService`, `MailAccountService`,
…). Two JSON/non-HTML responses exist: `POST /chats/refresh` and the SSE
`/chats/events`. A future `/api/v1` layer must call the same services.

## Background work
`@Scheduled` mailbox polling (`MailboxPollingScheduler`), IMAP IDLE
(`ImapIdleService`, flag `mailbox.idle.enabled`), digest/activation/
reengagement/trial-end schedulers.

## Tests
JUnit 5 + H2 (`src/test/resources/application.yml`), GreenMail, Testcontainers.
`./mvnw test`.
