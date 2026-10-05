# BACKEND — architecture (reverse-engineered 2026-10-05)

Spring Boot 3 / Java 21 monolith ("ConexusMail", a.k.a. MailIM in `master/PLAN.md`).
It serves **server-rendered Thymeleaf pages only**. There is currently **no JSON
REST API** for a mobile client (see `docs/API.md`).

> Deviation from the generic routine assumptions: persistence is **Spring Data
> JPA/Hibernate**, not jOOQ. There is no jOOQ codegen step. Flyway + PostgreSQL
> (H2 in tests) is as assumed.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Spring Security config (form login + Google OIDC + remember-me), registration, password reset, email verification, account settings, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook gateways, plan limits (`PlanLimitService`), trial nudges, cancel-reason capture |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`, `JakartaImapClient`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, credential encryption, conversation-key backfill |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService`, view-model records (`ChatConversation`, `BubbleRun`, …) |
| `domain` / `repository` | JPA entities and Spring Data repositories |
| `team` | Team invites/membership |
| `digest`, `activation`, `reengagement` | Scheduled emails (weekly digest, activation drip, re-engagement) |
| `admin` | Operator revenue/churn/win-back dashboard (`/admin/revenue`) |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)

Core: `users`, `subscriptions`, `mail_accounts` (IMAP password or Gmail OAuth,
encrypted credentials, `last_seen_uid`, `last_seen_sent_uid`, signature),
`email_thread` (owner-scoped, `conversation_key` = participant-set key), `message`
(+ recipients, outbound flag), `participant`, `attachment` (metadata only;
`blob_ref` is always null), `saved_searches`, `teams`/`team_members`/`team_invites`,
`auth_events`, `plan_change_events`, token tables, digest preferences.

A **conversation** in the UI = all threads sharing a `conversationKey`
(participant set), merged into one timeline; ownership is scoped per user and
optionally per mailbox address.

## Auth model

- Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,
  `password`) or Google OIDC (`/auth/google/start`); remember-me cookie (30 d).
- **CSRF is enabled** for all POSTs except `/billing/webhook` and `/h2-console/**`.
- `anyRequest().authenticated()`; public allow-list is in `SecurityConfig`.
- No bearer/JWT/API-key mechanism exists. A mobile client cannot authenticate
  without a new token scheme — see `INTERNAL_TODO.md`.

## Live updates

`ChatEventStream` (SSE, `GET /chats/events`) emits "new mail" signals published
by `MailboxPollingService` / `ImapIdleService` (paid tiers). The web client
reacts by refreshing HTML fragments.

## Web ↔ API relationship

All business logic lives in `service` / `web.*Service` / feature services.
Controllers are thin adapters that call services and populate Thymeleaf models.
A future REST layer must reuse the same services (`ConversationListService`,
`ChatService`, `OutboundMessageService`, `MailAccountService`, …).
