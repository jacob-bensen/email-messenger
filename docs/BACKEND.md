# Backend architecture

Classification: BACKEND repo (pom.xml, Spring Boot, Flyway migrations). Reverse-engineered 2026-11-06.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (NOT jOOQ — no codegen exists), PostgreSQL 16 (H2 in tests),
Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP).

## Packages (`com.emailmessenger`)
- `auth` — registration, form login, Google OIDC, password reset, email verification, account pages, login throttling.
- `billing` — Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges.
- `email` — mailbox connect (IMAP password, Gmail OAuth), import (`EmailImportService`), polling, IMAP IDLE.
- `service` — IM transform, conversation key, reply, chat view models.
- `web` — Thymeleaf controllers (`/chats`, `/mailboxes`, pricing, legal, PWA, SEO), `ChatEventStream` (SSE).
- `domain` / `repository` — JPA entities and repositories. `team`, `digest`, `admin`, `activation`, `reengagement` — feature modules.

## Schema overview
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients,
participants, attachments (`blob_ref` always null today), saved_searches, thread_notes, teams, team_members,
team_invites, digest_email_preferences, auth_events, email_verifications, password_reset_tokens, persistent_logins.
Conversations are keyed by participant-set `conversationKey`.

## Auth model
Session cookie (form login or Google OIDC), remember-me (`persistent_logins`), CSRF enabled except `/h2-console/**`
and `/billing/webhook` (Stripe signature instead). `anyRequest().authenticated()` with a public allow-list
(see `SecurityConfig`). No token/Bearer auth exists — **a mobile client cannot authenticate yet**.

## Web vs API
There is no REST API. All pages are server-rendered; the only JSON/stream endpoints are `POST /chats/refresh`
(`{"imported":n}`) and SSE `GET /chats/events`. See `API.md`, `WEB.md`.
