# Backend architecture (reverse-engineered 2026-10-29)

Spring Boot 3 / Java 21 monolith ("ConexusMail"). **Persistence is Spring Data JPA + Flyway (V1..V35), not jOOQ.**
PostgreSQL in prod/local, H2 in tests. Thymeleaf server-rendered web UI; there is **no REST/JSON API for a mobile client yet** (see `API.md`).

## Packages (`com.emailmessenger`)
- `web/` – Thymeleaf controllers (chats, mailboxes, saved searches, pricing/landing, legal, PWA, SEO), SSE stream `ChatEventStream`.
- `service/` – conversation building (`ConversationService`, `ChatService`), IM transform (`IMTransformService`, `EmailBodyCleaner`), `ReplyService`, conversation keys.
- `email/` – IMAP/Gmail import, polling (`MailboxPollingService`), IDLE (`ImapIdleService`), MIME parsing, credential encryption.
- `auth/` – form login, Google OIDC, registration, email verification, password reset, login throttling, account pages.
- `billing/` – Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges.
- `team/`, `digest/`, `admin/`, `activation/`, `reengagement/` – teams/invites, weekly digests, admin metrics, schedulers.
- `domain/`, `repository/` – JPA entities and repositories.

## Schema overview
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants,
attachments (`blob_ref` always null today), saved_searches, teams, team_members, team_invites, digest_email_preferences,
email_verifications, password_reset_tokens, persistent_logins, auth_events, thread_notes. Latest migration: `V35__mail_account_signature.sql`.

## Auth model
Session cookie (JSESSIONID) + optional 30-day remember-me; form login (`POST /login`, params `email`,`password`) and Google OIDC.
CSRF enabled except `/billing/webhook` (Stripe signature). Everything not in the permitAll list in `SecurityConfig` requires auth.
Stripe webhook authenticity = signature header. No token/bearer auth exists → a mobile client needs a new auth scheme (TBD, see `INTERNAL_TODO.md`).

## Web vs API
All routes are HTML except: `POST /chats/refresh` (JSON `{imported:int}`), `GET /chats/events` (SSE), `POST /billing/webhook`,
PWA/SEO assets. Controllers delegate to services; business rules live in services (billing, plan limits, import).
