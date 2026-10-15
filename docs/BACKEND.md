# Backend architecture (ConexusMail)

Reverse-engineered 2026-10-15. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (`ddl-auto: validate`,
NOT jOOQ), Flyway (V1..V35), PostgreSQL (prod/local), H2 (tests), Thymeleaf,
Spring Security (form login + Google OIDC + remember-me), Stripe, Jakarta Mail
(IMAP/SMTP), Maven.

## Modules (`com.emailmessenger.*`)
- `auth` — registration, login, Google OIDC, password reset, email
  verification, account settings, login throttle, auth audit events.
- `billing` — Stripe checkout/portal/webhook, plans (FREE/PRO/BUSINESS),
  `PlanLimits` (mailboxes, threads, saved searches), banners, trial/win-back.
- `email` — IMAP import (`MailboxPollingService`, `EmailImportService`,
  `MimeMessageParser`), IMAP IDLE, Gmail OAuth, credential encryption.
- `service` — conversation model: `ConversationService`, `IMTransformService`,
  `ConversationKeyService`, `ReplyService` (SMTP), view records
  (`ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`).
- `web` — Thymeleaf controllers (`ConversationController` = /chats workspace,
  `MailboxController`, `GmailConnectController`, `SavedSearchController`,
  pricing/legal/PWA/SEO), `ChatService`, `ConversationListService`,
  `OutboundMessageService`, SSE `ChatEventStream`.
- `team`, `digest`, `admin`, `activation`, `reengagement` — teams/invites,
  digests, admin metrics, lifecycle emails.
- `domain` / `repository` — JPA entities and Spring Data repositories.

## Core model
`conversationKey` = deterministic key over the participant set (excluding the
owner's own addresses). A "chat" merges every `EmailThread` with the same key
into one timeline of bubble runs. Messages: `Message`, `Participant`,
`Attachment` (`blob_ref` always null: bytes not stored), `MailAccount`
(per-user mailbox, encrypted creds, `auth_type` IMAP/OAuth).

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30d), CSRF enabled
(ignored only for `/h2-console/**`, `/billing/webhook`). All routes require
auth except the public list in `SecurityConfig`. **There is no token/Bearer
auth** — not usable from a mobile client yet (see INTERNAL_TODO).

## Web vs REST
Only HTML (Thymeleaf) endpoints exist, plus tiny JSON/SSE helpers
(`POST /chats/refresh`, `GET /chats/events`, `POST /billing/webhook`).
No `/api/**` namespace exists yet. See `docs/API.md`.
