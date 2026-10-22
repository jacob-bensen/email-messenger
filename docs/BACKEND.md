# BACKEND.md — architecture

Reverse-engineered 2026-10-22 from the code. Anything not verifiable is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (`ddl-auto: validate`, `open-in-view: false`),
PostgreSQL 16 (H2 in tests), Flyway (`V1`..`V35`), Thymeleaf, Maven wrapper. **No jOOQ** — the
generic routine's jOOQ assumption does not apply here; repositories are Spring Data (`repository/`, plus
`@Query` JPQL).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, Gmail connect, pricing, PWA, SEO, legal, saved searches), SSE (`ChatEventStream`), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | Email → IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`, `ReplyService`; view-model records (`ChatConversation`, `BubbleRun`, `BubbleMessage`, `ConversationListItem`) |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), Gmail OAuth, polling (`MailboxPollingService/Scheduler`), import (`EmailImportService`, `MimeMessageParser`), credential encryption |
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, tokens, `AuthEvent`, `PlanChangeEvent` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 msgs; PRO 5; BUSINESS unlimited), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, activation + win-back emails, admin revenue page |

## Domain core
Conversations are keyed by `conversationKey` = normalized participant set (excluding the owner's
addresses), computed by `ConversationKeyService` and stored on `email_thread` (V32). A chat merges all
threads sharing a key into one timeline of `BubbleRun`s (consecutive same-sender messages).
Owner-scoped everywhere: threads belong to a `User` (V3).

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 d, persistent-token table) via form login
(`/login`, params `email`/`password`) or Google OIDC. CSRF is **on** (ignored only for `/h2-console/**`
and `/billing/webhook`). Login throttle filter precedes form login. Everything not in the permit-list in
`SecurityConfig` requires authentication. **There is no token/bearer authentication** (see INTERNAL_TODO).

## Web vs REST
All HTTP surface today is server-rendered Thymeleaf plus a few non-HTML endpoints (SSE `/chats/events`,
Stripe webhook, PWA/SEO assets). **No JSON REST API for the mobile client exists.** See `API.md`.
Controllers delegate to services (`ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`); a future `/api/v1` layer must reuse the same services.

## Schema overview (Flyway)
V1 init (threads/messages/participants/attachments) · V2 users · V4 subscriptions · V5–V7,V29,V31,V35 mail
accounts (auth type, UID cursors, polling policy, signature) · V9 unread · V10–V11 saved searches ·
V12 digests · V13,V20–V23,V28 lifecycle email tracking · V14–V16 reset/verify/auth events · V18–V19
Google subject/password-set · V24 teams · V26–V27 plan change/cancel reason · V30 outbound messages ·
V32 conversation key · V34 user mail prefs. (V25 thread notes added then dropped in V33.)
Note: `attachments.blob_ref` is always null — attachment bytes are not stored.

## Background jobs
`@Scheduled` polling (`mailbox.polling.enabled`), IMAP IDLE for paid plans (`mailbox.idle.enabled`),
digest/activation/trial schedulers. Live updates reach browsers over SSE; `NewMailEvent` is the hook.
