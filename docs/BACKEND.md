# BACKEND

Classification: BACKEND (pom.xml, Spring Boot, Flyway migrations V1–V35 in `src/main/resources/db/migration`).
Note: persistence is **Spring Data JPA/Hibernate** (`ddl-auto: validate`), not jOOQ.

## Modules (`com.emailmessenger.*`)
- `web/` Thymeleaf controllers (chats, mailboxes, saved searches, pricing, PWA, legal, SSE `ChatEventsController`).
- `service/` conversation building, IM transform, reply sending (`ConversationService`, `IMTransformService`, `ReplyService`).
- `email/` IMAP/Gmail import, polling, IDLE (`MailboxPollingService`, `ImapIdleService`, `EmailImportService`).
- `auth/` form login + Google OIDC, registration, password reset, email verification, throttling.
- `billing/` Stripe checkout/portal/webhook, plan limits (`Plan` = FREE, PRO, BUSINESS).
- `team/`, `digest/`, `admin/` (revenue/churn metrics, `/admin/revenue`), `activation/`, `reengagement/`.
- `domain/` JPA entities; `repository/` Spring Data repositories.

## Schema overview
Core: `User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment` (`blob_ref` always null today), `SavedSearch`, `Subscription`, `PlanChangeEvent`, `Team*`, `AuthEvent`. Conversations are keyed by a participant-set `conversationKey`.

## Auth model
Session cookie (JSESSIONID) + remember-me (30 days), CSRF enabled (only `/billing/webhook` and `/h2-console/**` exempt). Form login at `POST /login` (params `email`, `password`); Google OIDC optional. No token/Bearer auth exists. Unauthenticated requests redirect to `/login`.

## Web vs REST
There is currently **no REST/JSON API**. All endpoints are server-rendered HTML forms, plus one SSE stream (`/chats/events`). See `API.md`, `WEB.md`.
