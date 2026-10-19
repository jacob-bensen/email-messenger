# Backend architecture

Repo classification: **BACKEND** (pom.xml, Spring annotations, Flyway migrations
V1–V35). Sibling: `conexus-mail-mobile` (Flutter client, currently an empty stub).

> Deviation from the generic target stack: persistence is **Spring Data JPA /
> Hibernate** (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Data JPA, Flyway, PostgreSQL 16
(H2 in tests), Spring Security (form login + optional Google OIDC), Stripe,
Jakarta Mail (IMAP/SMTP, IMAP IDLE).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, landing/legal/SEO/PWA), `ChatEventStream` SSE hub |
| `service` | Conversation building, IM transform (`EmailBodyCleaner`, `IMTransformService`), `ReplyService`, `ConversationKeyService` |
| `email` | IMAP/Gmail OAuth import, polling, IDLE, MIME parsing, credential encryption |
| `auth` | Users, login/registration, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (tables)
`users`, `persistent_logins`, `subscriptions`, `plan_change_events`,
`mail_accounts` (IMAP password or Gmail OAuth, polling policy, last-seen UIDs,
signature), `participants`, `email_threads` (owner, unread, `conversation_key`),
`messages` (incl. outbound flag), `message_recipients`, `attachments`
(`blob_ref` always null today), `saved_searches`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `teams`,
`team_members`, `team_invites`. Migrations are immutable; add `V36+`.

## Auth model
Session cookie (JSESSIONID) + 30-day persistent remember-me, CSRF enabled
(exempt: `/billing/webhook`, `/h2-console/**`). Public paths are whitelisted in
`SecurityConfig`; everything else requires authentication. Login throttling via
`LoginThrottleFilter`. **No token/Bearer auth exists** — required for mobile
(see INTERNAL_TODO).

## Web vs REST
All HTTP surface today is server-rendered Thymeleaf (see `WEB.md`) plus three
non-HTML endpoints (`POST /chats/refresh` JSON, `GET /chats/events` SSE,
`POST /billing/webhook`). There is **no REST API for the mobile client**; see
`API.md`. When one is added it must call the same services the controllers use.

## Conventions
See `CLAUDE.md`.
