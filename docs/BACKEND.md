# BACKEND architecture

Reverse-engineered 2026-10-29 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3.5.x, Thymeleaf, Spring Data **JPA/Hibernate** (not jOOQ — no jOOQ
codegen exists), Flyway (V1–V35), PostgreSQL 16 (H2 in tests), Maven wrapper (`./mvnw`).

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account pages, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | Mailbox connect (IMAP password, Gmail OAuth), polling, IMAP IDLE, MIME parsing, import, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keying, `ReplyService`, chat view models |
| `web` | Thymeleaf controllers, SSE (`/chats/events`), PWA, SEO, legal, model advices |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, operator metrics |

## Data model (see `db/migration`)
`users`, `subscriptions`, `mail_accounts` (auth_type, last_seen_uid, last_seen_sent_uid, signature),
`email_thread` (owner, unread, `conversation_key` = participant-set key), `message`
(outbound flag), `message_recipient`, `participant`, `attachment` (`blob_ref` always null),
`saved_searches`, `teams`/`team_members`/`team_invites`, token/audit/preference tables.

## Auth model
Server-side session (`JSESSIONID`) + remember-me cookie, **CSRF enabled** (exempt only
`/billing/webhook`, `/h2-console/**`). Everything not whitelisted in `SecurityConfig` requires
authentication. Unauthenticated requests are redirected to `/login` (HTML), not 401 JSON.
There is **no token/JWT mechanism**, so a mobile client cannot authenticate today.

## Web vs REST
The only REST-style endpoint is `POST /billing/webhook` (Stripe). All user features are
server-rendered Thymeleaf controllers that call services directly. No JSON API exists
(see `docs/API.md`, `INTERNAL_TODO.md`).
