# BACKEND.md — architecture

Reverse-engineered from code on 2026-11-05. Items not verifiable from code are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (`ddl-auto: validate`; **not jOOQ**), PostgreSQL (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security, Maven wrapper. Deviation from the routine's "jOOQ" assumption is recorded in INTERNAL_TODO.md; JPA is kept (CLAUDE.md convention: match existing architecture).

## Packages (`com.emailmessenger`)
- `web/` — Thymeleaf controllers (chats, mailboxes, saved searches, pricing/landing, legal, PWA, SEO), `ChatEventStream` (SSE), `GlobalExceptionHandler`.
- `service/` — IM transform (`IMTransformService`, `EmailBodyCleaner`), conversations (`ConversationService`, `ConversationKeyService`), `ReplyService`.
- `email/` — IMAP import/polling/IDLE, Gmail OAuth, credential encryption, MIME parsing.
- `auth/` — form login + Google OIDC, registration, verification, password reset, throttling, account pages.
- `billing/` — Stripe checkout/portal/webhook, plan limits (`PlanLimitService`).
- `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` — teams, digests, drip emails, operator revenue/churn metrics.
- `domain/`, `repository/` — JPA entities and Spring Data repos.

## Schema overview
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads (has `conversation_key`), messages (has outbound flag, V30), message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, teams, team_members, team_invites, digest_email_preferences, password_reset_tokens, email_verifications, auth_events, persistent_logins. Conversation = participant-set `conversationKey` across threads.

## Auth model
Session cookie (form login or Google OIDC), remember-me (`persistent_logins`). CSRF enabled except `/h2-console/**` and `/billing/webhook`. Everything not in the permitAll list in `auth/SecurityConfig.java` requires authentication. **There is no token/API auth for a mobile client yet.**

## Web vs API
All user-facing behaviour is server-rendered HTML via controllers that call services. The only JSON/non-HTML endpoints: `POST /billing/webhook` and the SSE stream `/chats/events`. No `/api/**` exists. See API.md.
