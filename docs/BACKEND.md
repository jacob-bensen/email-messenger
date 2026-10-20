# BACKEND architecture

Classification: BACKEND (pom.xml, Spring Boot, Flyway migrations in `src/main/resources/db/migration`).
Sibling FRONTEND: `conexus-mail-mobile` (Flutter; currently only a README).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring Data **JPA** (NOT jOOQ — no jOOQ dependency or codegen exists), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Maven wrapper.

## Packages (`com.emailmessenger`)
- `web/` — Thymeleaf controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` SSE).
- `service/` — conversation building (`ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`).
- `email/` — IMAP/Gmail import, polling, IDLE, MIME parsing.
- `auth/` — form login, Google OIDC, password reset, email verification, throttling, `SecurityConfig`.
- `billing/` — Stripe checkout/portal/webhook, plan limits (Free/Pro/Business).
- `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` — teams, digests, lifecycle emails, admin metrics.
- `domain/` — JPA entities; `repository/` — Spring Data repos.

## Schema overview (Flyway V1..V*; immutable once merged)
users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants, attachments, thread_notes, saved_searches, teams, team_members, team_invites, auth_events, password_reset_tokens, email_verifications, digest_email_preferences, persistent_logins.
Conversations are keyed by participant-set `conversationKey` (see `ConversationKeyService`).

## Auth model
Session cookie (JSESSIONID) + persistent remember-me; CSRF **enabled** (ignored only for `/h2-console/**`, `/billing/webhook`). Form login `POST /login` (`email`,`password`), optional Google OIDC. Everything not on the public allow-list in `SecurityConfig` requires authentication. There is **no token/bearer auth** — a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs API
All HTTP endpoints are server-rendered `@Controller`s. Only JSON endpoints: `POST /chats/refresh` → `{"imported":n}` and Stripe `POST /billing/webhook`; plus SSE `GET /chats/events`. There is no REST API for the mobile client yet. Controllers delegate to services (`ChatService`, `MailAccountService`, `BillingService`, …); some mailbox-selection helpers (`selectMailbox`, `scopeAddress`) live in `ConversationController` and should move to a service before a REST controller reuses them.
