# BACKEND.md — architecture

Repo classification: BACKEND (pom.xml, Spring annotations, Flyway migrations V1–V35).
Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently only a README).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring Data **JPA** (NOT jOOQ — differs from the routine's
assumed stack; no codegen step), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf,
Spring Security (form login + Google OIDC + remember-me), Stripe, Jakarta Mail
(IMAP/SMTP), jsoup. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, searches, legal/SEO/PWA |
| `auth` | Security config, register/login/reset/verify, account settings, Google OAuth |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team` | Team invites/membership |
| `email` | IMAP import, polling, IDLE, Gmail OAuth, MIME parsing |
| `service` | Conversation building, IM transform, body cleaning, reply/outbound |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `admin`, `digest`, `activation`, `reengagement` | metrics + scheduled emails |

## Schema overview
Core: `users`, `subscriptions` (Plan FREE/PRO/BUSINESS), `mail_accounts`
(auth_type IMAP password | Gmail OAuth, encrypted credentials, last-seen UIDs),
`email_threads` (owner, `conversation_key` = participant-set key), `messages`
(outbound flag), `participants`, `message_recipients`, `attachments`
(`blob_ref` always null today), `saved_searches`, `teams`/`team_members`/`team_invites`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `plan_change_events`,
`digest_email_preferences`. Migrations are immutable; add a new `V{n}`.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token repo).
CSRF **enabled** (exempt: `/h2-console/**`, `/billing/webhook`). Login: POST `/login`
(`email`, `password`). Login throttling filter precedes form login. Google OIDC only
when `GOOGLE_CLIENT_ID` is set. **No token/Bearer auth exists** — see INTERNAL_TODO.

## Web vs REST
All HTTP surface is server-rendered Thymeleaf (see WEB.md) except three JSON/stream
endpoints (`POST /chats/refresh`, `GET /chats/events` SSE, `POST /billing/webhook`).
There is **no REST API for the mobile client yet**. Controllers delegate to services
(`ConversationListService`, `ConversationService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`...), so a future `/api/v1` layer should
reuse those, not controllers.
