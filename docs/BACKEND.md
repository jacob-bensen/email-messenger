# BACKEND.md — architecture

Repo classification: BACKEND (pom.xml, Spring Boot, Flyway migrations V1–V34). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently an empty scaffold).

Reality vs. the sync-routine's assumed stack: persistence is **Spring Data JPA/Hibernate**, not jOOQ (no codegen step). Build is Maven, Java 21, Spring Boot 3, PostgreSQL 16 (H2 in tests), Flyway.

## Packages (`com.emailmessenger`)
- `web/` — Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, PWA, SEO, legal) + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` SSE).
- `service/` — IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService` (participant-set key), `ReplyService`.
- `domain/`, `repository/` — JPA entities and Spring Data repositories.
- `email/` — IMAP/Gmail import, polling (`MailboxPollingService`), IDLE (`ImapIdleService`), credential encryption.
- `auth/` — form login, Google OIDC, remember-me, password reset, email verification, login throttle, `SecurityConfig`.
- `billing/` — Stripe checkout/portal/webhook, plan limits (`PlanLimitService`).
- `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` — teams/invites, digests, lifecycle emails, `/admin/revenue` metrics.

## Schema overview (Flyway, immutable)
users, subscriptions, mail_accounts, email_threads (owner, conversation_key), messages, message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, teams/team_members/team_invites, digest_email_preferences, password_reset_tokens, email_verifications, auth_events, plan_change_events, persistent_logins. `thread_notes` was dropped in V33.

## Auth model
Session cookie (form login or Google OIDC) + CSRF enabled; remember-me (30 d, persistent token table). Only `/billing/webhook` (Stripe signature) and H2 console are CSRF-exempt. **No bearer-token / mobile auth exists.**

## Web vs REST
The website is server-rendered; there is **no JSON REST API** for a mobile client. The only non-HTML endpoints: `POST /chats/refresh` (@ResponseBody), `GET /chats/events` (SSE), `POST /billing/webhook`, PWA/SEO assets. See `docs/API.md`, `docs/WEB.md`.
