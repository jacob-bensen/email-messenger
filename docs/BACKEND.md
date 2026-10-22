# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml + Spring annotations + Flyway migrations V1–V35). Sibling: `conexus-mail-mobile` (Flutter, FRONTEND).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, **Spring Data JPA** (not jOOQ — no codegen step), PostgreSQL (H2 in tests), Flyway, Maven wrapper, Jakarta Mail (IMAP/SMTP), Stripe.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing/landing, legal, PWA, SEO), SSE `ChatEventsController`, view services (`ConversationListService`, `ChatService`, `OutboundMessageService`) |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, credential encryption |
| `auth` | Security config, registration/login/reset/verify, Google OIDC, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Data model (overview)
`users`, `subscriptions`, `mail_accounts` (IMAP password or Gmail OAuth; encrypted credentials), `email_thread` (owner + `conversation_key` = participant-set key), `message` (+ recipients, attachments — `blob_ref` currently always null), `participant`, `saved_searches`, `teams`/`team_members`/`team_invites`, token tables (password reset, email verify), `auth_events`, `plan_change_events`. Migrations are immutable; add new `V{n}`.

## Auth model
Session cookie (form login `POST /login`, fields `email`/`password`; optional Google OIDC) + remember-me cookie (30d) + **CSRF enabled** (only `/h2-console/**` and `/billing/webhook` exempt). Everything not on the public allow-list in `auth/SecurityConfig` requires login. Login is throttled (`LoginThrottleFilter`). **There is no token/JWT auth and no REST API today** — see `docs/API.md`.

## Web vs API
Controllers resolve the user from `Principal`, call services, and return view names. Business rules live in services (plan limits in `PlanLimitService`, sending in `OutboundMessageService`/`ReplyService`), so a future JSON API should call these same services.
