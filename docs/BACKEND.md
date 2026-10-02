# BACKEND.md — architecture

Reverse-engineered 2026-10-02 from the code. Anything not implemented is marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ — no jOOQ dependency, no codegen), PostgreSQL 16
(H2 in tests), Flyway (`src/main/resources/db/migration`, V1–V35), Thymeleaf, Spring Security, Stripe, Jakarta Mail.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account settings, login throttle/audit |
| `billing` | Plans (FREE/PRO/BUSINESS), `PlanLimits`, Stripe checkout/portal/webhook, trial nudges |
| `email` | Mailbox connect (IMAP password, Gmail OAuth), polling, IMAP IDLE, MIME parsing, import, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keying, `ReplyService`, view records |
| `web` | Thymeleaf controllers (chats workspace, mailboxes, searches, pricing, PWA, SEO, legal), SSE stream |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, admin metrics, lifecycle emails |

## Data model (tables)
users, subscriptions, plan_change_events, auth_events, persistent_logins, email_verifications, password_reset_tokens,
mail_accounts, email_threads, messages, message_recipients, participants, attachments, thread_notes, saved_searches,
digest_email_preferences, teams, team_members, team_invites. Conversations are keyed by a participant-set
`conversationKey` (see `ConversationKeyService`), spanning multiple email threads.

## Auth model
Session cookie (`JSESSIONID`) + 30-day persistent remember-me; CSRF **enabled** (exempt: `/h2-console/**`,
`/billing/webhook`). Public paths are listed in `auth/SecurityConfig.java`; everything else requires login.
**There is no token/Bearer auth** — the mobile client cannot authenticate yet (see INTERNAL_TODO).

## Web vs. REST
Today the app is server-rendered only. There is **no REST API for a mobile client**. The only JSON/non-HTML
endpoints are `/chats/refresh`, `/chats/events` (SSE), and the Stripe webhook; see `API.md`. Business logic is partly
in services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `ReplyService`) but the send
orchestration (signature, sender account, reply-vs-new-thread choice) lives in `ConversationController.send` and must
be extracted into a service before an API reuses it.
