# BACKEND architecture

Reverse-engineered 2026-10-14 from the code. Anything not implemented is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, **Spring Data JPA** (not jOOQ),
Flyway (V1..V35 in `src/main/resources/db/migration`), PostgreSQL (H2 in tests), Maven.

## Packages (`com.emailmessenger`)
- `web` – Thymeleaf controllers: chats workspace, mailboxes, saved searches, PWA, SEO, legal, SSE.
- `auth` – form login, Google OIDC, registration, password reset, email verification,
  login throttling, remember-me (persistent tokens).
- `billing` – Stripe checkout/portal/webhook, plan limits (`PlanLimitService`).
- `email` – IMAP import/polling/IDLE, Gmail OAuth, credential encryption.
- `service` – parsing, IM transform, threading, reply/compose.
- `domain` / `repository` – JPA entities and Spring Data repositories.
- `team`, `digest`, `activation`, `reengagement`, `admin` – teams, digests, lifecycle emails, metrics.

## Schema overview
Tables: users, persistent_logins, subscriptions, plan_change_events, mail_accounts,
participants, email_threads (owner, conversation_key, unread), messages (outbound flag),
message_recipients, attachments (blob_ref always null), saved_searches, teams, team_members,
team_invites, digest_email_preferences, password_reset_tokens, email_verifications, auth_events.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`) or
Google OIDC; optional 30-day remember-me. **CSRF is enabled** (only `/billing/webhook` and
`/h2-console/**` exempt). Everything not on the public allow-list requires authentication.
There is **no token/bearer auth** and **no JSON API** (see `docs/API.md`).

## Web vs API
All user-facing behavior is server-rendered. No `/api/**` exists, so there is currently no
duplicated web/API logic; when an API is added it must call the same services the controllers use.
