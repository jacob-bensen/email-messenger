# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website and (future) mobile API live in the same app and must share services.

> Reality check (2026-10-30): the stack is **Spring Data JPA + Flyway +
> PostgreSQL** (H2 in tests), not jOOQ. There is **no REST API for the mobile
> client yet** — see `API.md`.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | form login, Google OIDC, registration, verification, password reset, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | mailbox connect (IMAP password / Gmail OAuth), `MailboxPollingService`, IMAP IDLE, MIME parsing, import |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService` |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, landing, legal, PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `team`, `digest`, `admin`, `activation`, `reengagement` | teams/invites, digests, admin metrics, lifecycle emails |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (Flyway V1–V35, see `src/main/resources/db/migration`)
users, subscriptions, plan_change_events, mail_accounts (auth type, polling
policy, last-seen UIDs, signature), email_thread (owner, unread,
`conversation_key`), message (+ outbound flag), message_recipient, participant,
attachment (`blob_ref` always null today), saved_searches, digest prefs, teams /
invites, auth_events, verification & reset tokens.

A **conversation** = participant-set `conversationKey`; threads (by
Message-ID/In-Reply-To/References) merge into one chat timeline.

## Auth model
Session cookie (form login or Google OIDC) with CSRF on all mutating requests
except `/billing/webhook`. All routes are authenticated except public pages
(see `WEB.md`). Users are resolved via `Principal.getName()` = email.
**TBD:** token auth for mobile (needed before any `/api/v1`).

## Web ↔ API relationship
Controllers resolve the `User`, then call services (`ChatService`,
`ConversationListService`, `OutboundMessageService`, `MailAccountService`,
`PlanLimitService`). A future `/api/v1` controller package must reuse those
services; the only logic currently inside a controller is mailbox selection /
scope resolution in `ConversationController` (see `INTERNAL_TODO.md`).
