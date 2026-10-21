# BACKEND

Spring Boot 3 / Java 21 app ("ConexusMail", package `com.emailmessenger`). Server-rendered
Thymeleaf website + billing + mailbox import. Persistence is **Spring Data JPA** (Hibernate,
`ddl-auto: validate`) over PostgreSQL 16 with **Flyway** migrations (`V1`…`V35`); H2 in tests.
(The generic routine assumes jOOQ — it is NOT used here; do not introduce it.)

## Packages
- `web/` – controllers (`ConversationController` for /chats, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `PwaController`, `SeoController`, `LegalController`, `ChatEventsController`), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`).
- `service/` – IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService` (SMTP send), view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, `ChatMember`).
- `email/` – IMAP import/polling/IDLE, Gmail OAuth, credential encryption.
- `auth/` – Spring Security config, registration, password reset, email verification, account pages, Google OIDC.
- `billing/` – Stripe checkout/portal/webhook, plan limits (`Plan`: FREE/PRO/BUSINESS).
- `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` – teams/invites, weekly digests, lifecycle emails, operator metrics.
- `domain/`, `repository/` – JPA entities / Spring Data repos.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,`password`), optional
remember-me (30d, persistent token table), Google OIDC login when `GOOGLE_CLIENT_ID` is set.
CSRF is ON (exempt: `/h2-console/**`, `/billing/webhook`). Everything not on the permitAll list in
`auth/SecurityConfig` requires authentication. **There is no token (bearer/JWT) auth.**

## Key data
`users`, `subscriptions`, `mail_accounts` (encrypted IMAP creds / Gmail OAuth tokens),
`email_threads`, `messages`, `message_recipients`, `participants`, `attachments`
(`blob_ref` always null — bytes not stored), `saved_searches`, `teams`/`team_members`/`team_invites`,
`digest_email_preferences`, `auth_events`, `plan_change_events`, token tables. Conversations are
keyed by participant-set `conversationKey` (`ConversationKeyService`).

## Web ↔ API relationship
Today the website is the only client. Controllers return Thymeleaf views; the only JSON/stream
endpoints are `POST /chats/refresh`, `GET /chats/events` (SSE) and the Stripe webhook. A mobile
JSON API (`/api/v1/**`) is **planned, not implemented** — see `API.md` §Planned. It must call the
same services (`ConversationListService`, `ChatService`, `ReplyService`/`OutboundMessageService`,
`MailAccountService`, `BillingService`); note several of those are package-private in `web/`.

## Pre-existing project docs
`CLAUDE.md` (roadmap), `master/PLAN.md`, `master/BACKLOG.md`, `master/CHANGELOG.md` (older
routine's changelog), `DEPLOY.md`.
