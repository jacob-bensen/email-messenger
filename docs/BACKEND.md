# BACKEND.md — architecture

Reverse-engineered 2026-10-29 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ — no jOOQ dependency in `pom.xml`),
Flyway (`src/main/resources/db/migration`, V1..V35), PostgreSQL (prod/local), H2 (tests),
Thymeleaf, Spring Security (form login + Google OIDC, remember-me), Stripe, Jakarta Mail (IMAP/SMTP).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, saved searches, pricing/landing, PWA, SEO, legal), view models |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService` |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, register/login/reset/verify, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digest emails, onboarding nudges, win-back, admin revenue metrics |

## Schema overview
Core: `users`, `mail_account`, `email_thread`, `message`, `message_recipient`, `participant`,
`attachment` (`blob_ref` always null today), `saved_search`. Billing: `subscription`, `plan_change_event`.
Teams: `team`, `team_member`, `team_invite`. Auth: `email_verification_token`, `password_reset_token`,
`auth_event`, remember-me tokens. Other: `digest_email_preference`. Conversations are keyed by a
participant-set `conversationKey`. See migrations for exact DDL.

## Auth model
Session cookie (form login `/login`, params `email`/`password`; Google OIDC via `/auth/google/start`),
remember-me 30 days, CSRF **enabled** (exempt: `/h2-console/**`, `/billing/webhook`).
Public paths are the allow-list in `auth/SecurityConfig.java`; everything else requires authentication.
There is **no token/bearer auth** — a mobile client cannot authenticate today.

## Web vs REST
The app is entirely server-rendered. The only JSON/non-HTML endpoints are `POST /chats/refresh`,
`POST /billing/webhook`, and SSE `GET /chats/events`. There is no REST API; see `API.md`.
Controllers delegate to services (`ChatService`, `ConversationService`, `ReplyService`,
`MailAccountService`); no divergent logic found because only one consumer exists.
