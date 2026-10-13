# BACKEND.md — ConexusMail server architecture

Reverse-engineered from the code on 2026-10-13. Anything not verified is marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3.5.x, Spring Data **JPA/Hibernate** (not jOOQ — no codegen step), Flyway
(`src/main/resources/db/migration`, V1–V35), PostgreSQL (prod/local), H2 (tests), Thymeleaf,
Spring Security (form login + Google OIDC, remember-me), Stripe, Jakarta Mail (IMAP/SMTP),
Maven wrapper. Root package `com.emailmessenger`.

## Modules (packages)
| Package | Role |
|---|---|
| `domain` | JPA entities: User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, tokens, audit events |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), credential encryption, Gmail OAuth |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models (`ChatConversation`, `BubbleRun`), `ConversationKeyService`, `ReplyService` |
| `web` | Thymeleaf controllers, `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE stream (`ChatEventStream`), PWA/SEO controllers, mailbox + saved-search management |
| `auth` | Security config, registration/login/reset/verify, account settings, Google OIDC provisioning, login throttling |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), banners, trial conversion |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest mails, lifecycle emails, operator revenue metrics |

## Domain core
A **conversation** is keyed by `conversationKey` (participant-set hash, V32) and merges all
`EmailThread`s with the same participants. `/chats/{key}` is the unit the UI works with.
Outbound messages are flagged (V30). Sent-folder sync uses `last_seen_sent_uid` (V31).

## Auth model
Session-cookie auth (JSESSIONID + persistent remember-me, 30 days) with **CSRF enabled**
(only `/h2-console/**`, `/billing/webhook` exempt). Everything except the public allowlist in
`SecurityConfig` requires authentication. There is **no token/bearer auth** and no `/api/**`.

## Web vs. API
There is currently **no REST API**. All routes are server-rendered Thymeleaf (see `WEB.md`);
the only JSON/stream endpoints are `/billing/webhook` (Stripe) and `/chats/events` (SSE).
A mobile client therefore has no contract to call — see `API.md` and `INTERNAL_TODO.md`.
Controllers already delegate to services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, `BillingService`), but `ConversationController`
still holds orchestration logic (send/reply branching, mailbox scoping, attachment mapping) that
must be extracted into a service before a REST controller can reuse it.
