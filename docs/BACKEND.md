# BACKEND

Spring Boot 3 / Java 21 monolith ("ConexusMail", package `com.emailmessenger`).
Serves the Thymeleaf website **and** (planned) the REST API for the mobile
client. Last audited: 2026-10-02.

> Stack note: the routine template assumes jOOQ. This repo uses **Spring Data
> JPA / Hibernate** (`ddl-auto: validate`) with **Flyway** migrations (V1–V35 in
> `src/main/resources/db/migration`). There is no jOOQ codegen step.

## Modules (`src/main/java/com/emailmessenger`)

| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial/billing banners |
| `email` | Mailbox integration: IMAP client, IMAP IDLE, Gmail OAuth, MIME parsing, import, polling, credential encryption |
| `service` | Domain services + view models: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`, bubble/chat records |
| `web` | Thymeleaf controllers, `ConversationListService`, `ChatService`, `OutboundMessageService`, SSE (`ChatEventStream`), PWA, SEO, saved searches |
| `team` | Teams and invites |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle / digest emails |
| `admin` | Operator revenue/funnel/churn dashboards and weekly digest |
| `domain` | JPA entities |
| `repository` | Spring Data repositories |

## Core model

A **conversation** is keyed by `conversationKey` = the participant set (see
`ConversationKeyService`), merged across email threads. Chats list rows are
`ConversationListItem`; an open chat is `ChatConversation` →
`SenderBubbleRun` → `BubbleMessage` (see `docs/API.md` for JSON shapes).

## DB schema overview (Flyway)

- Identity: `users` (V2, +V8 acquisition source, V18 google subject, V19
  password_set, V13/V20–22 lifecycle tracking, V34 mail prefs), `password_reset_tokens`
  (V14), `email_verifications` (V15), `auth_events` (V16), `persistent_logins`
  (remember-me).
- Billing: `subscriptions` (V4, V17, V23, V27, V28), `plan_change_events` (V26).
  Plans: `FREE`, `PRO`, `BUSINESS` (`domain/Plan`).
- Mail: `mail_accounts` (V5–V7, V29 auth_type, V31 sent cursor, V35 signature),
  `participants`, `email_threads` (V3 owner, V9 unread, V32 conversation_key),
  `messages` (V30 outbound), `message_recipients`, `attachments`
  (`blob_ref` is always null — bytes are not stored).
- Other: `saved_searches` (V10, V11), `digest_email_preferences` (V12),
  `teams`/`team_members`/`team_invites` (V24).

## Auth model

- Session-based (`JSESSIONID`) form login at `POST /login` (`email`, `password`),
  plus 30-day persistent remember-me and optional Google OIDC (enabled only
  when `GOOGLE_CLIENT_ID`/`SECRET` are set). `SecurityConfig` permits a fixed
  public-page list; **everything else requires authentication**.
- CSRF is **on**, except `/h2-console/**` and `/billing/webhook`.
- Login is throttled (`LoginThrottleFilter`) and audited (`auth_events`).
- **No token/JWT/API-key auth exists.** A mobile client cannot authenticate
  today other than by replaying the browser form-login + CSRF flow. See
  `INTERNAL_TODO.md` #1.

## Web vs. API

All current controllers are server-rendered MVC controllers. Only three
handlers return non-HTML: `POST /chats/refresh` (JSON), `GET /chats/events`
(SSE), `POST /billing/webhook`. Controllers call the service layer
(`ConversationListService`, `ChatService`, `OutboundMessageService`,
`MailAccountService`, …); a future `/api/v1/**` controller set MUST reuse
these services, not the controllers. Several controllers still hold selection
logic (`selectMailbox`, `scopeAddress`, `accountColorMap` in
`ConversationController`) that must move into a service before the API can
share it.

## Config

`application.yml` reads `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_*`, `STRIPE_*`
env vars; `local` profile → `localhost:5432/email_messenger`. Tests use H2.
