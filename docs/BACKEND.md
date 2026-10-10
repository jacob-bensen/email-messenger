# BACKEND.md — architecture

Reverse-engineered 2026-10-10 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ — no codegen step), PostgreSQL 16 (H2 in tests),
Flyway (`src/main/resources/db/migration`, V1..V35, immutable once merged), Thymeleaf, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, PWA, SEO, legal), SSE (`ChatEventStream`), model advices |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling, IMAP IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, admin metrics, lifecycle schedulers |

## Data model (tables)
users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients,
participants, attachments (`blob_ref` always null today), saved_searches, teams, team_members, team_invites,
digest_email_preferences, auth_events, email_verifications, password_reset_tokens, persistent_logins, thread_notes.
Conversations are keyed by a participant-set `conversationKey` (`ConversationKeyService`).

## Auth model
Server-side session (`JSESSIONID`) via form login (`POST /login`, params `email`,`password`) or Google OIDC;
remember-me (30 days). CSRF is ON for everything except `/h2-console/**` and `/billing/webhook`.
Everything not on the permit-all list in `SecurityConfig` requires authentication.
**No token/API-key auth exists** — a mobile client cannot authenticate non-interactively today (see INTERNAL_TODO).

## Web vs REST
There is **no REST API**. The only non-HTML responses: `POST /chats/refresh` (JSON), `GET /chats/events` (SSE),
`POST /billing/webhook` (Stripe). All business logic is in services; controllers are thin Thymeleaf endpoints.
See `API.md` and `WEB.md`.
