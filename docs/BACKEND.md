# BACKEND.md — architecture

Generated 2026-10-01 by reverse-engineering the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (NOT jOOQ — `ddl-auto: validate`),
PostgreSQL 16 (H2 in tests), Flyway (`V1`–`V35`), Thymeleaf, Spring Security, Maven wrapper.
Package root `com.emailmessenger`.

## Modules (packages)
| Package | Role |
|---|---|
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `SavedSearch`, `Subscription`, `Team*`, token/audit entities |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models (`ChatConversation`, `ConversationListItem`, `BubbleRun`, …), `ReplyService` (SMTP), `ConversationKeyService` |
| `web` | Thymeleaf controllers (`ConversationController` = /chats), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`), PWA/SEO controllers |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, login throttle |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (FREE/PRO/BUSINESS) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, `/admin/revenue` metrics |

## Domain core
A **conversation** is keyed by `conversationKey` (participant set, `ConversationKeyService`);
it merges all `EmailThread`s with that key into one timeline of `BubbleRun`s.

## Auth model
Browser session (form login, `email`/`password`, remember-me 30d) or Google OIDC. CSRF on
(exempt: `/billing/webhook`, `/h2-console/**`). Everything not whitelisted in
`SecurityConfig` is `authenticated()`. **There is no token/bearer auth and no JSON API** —
see `API.md`.

## Web vs REST
Only Thymeleaf pages exist, plus tiny `@ResponseBody` helpers (`POST /chats/refresh`,
SSE `/chats/events`). Send/compose orchestration (sender account choice, signature
append, recipient parsing, record outbound) lives **inside `ConversationController`**, not a
service — must be extracted before a REST API can share it (INTERNAL_TODO #2).

## Config
Env vars: `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_CLIENT_*`, `STRIPE_*`, credential encryption
key/salt (see `application.yml`, `.env.example`). Profile `local` for dev.
Note: `master/` holds a legacy operator log (PLAN/BACKLOG/CHANGELOG); its plan names
(Personal/Team/Enterprise) are stale vs code (FREE/PRO/BUSINESS).
