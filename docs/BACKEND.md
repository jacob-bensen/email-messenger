# Backend architecture

Repo classification: **BACKEND** (evidence: `pom.xml`, Spring Boot 3 annotations,
Flyway migrations `V1`–`V35`). Sibling client: `conexus-mail-mobile` (Flutter).

> Deviation from the generic routine's assumed stack: persistence is
> **Spring Data JPA/Hibernate, not jOOQ**. There is no jOOQ codegen. Do not
> introduce jOOQ for the mobile API; use the existing repositories/services.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Data JPA, Spring Security (form login,
remember-me, Google OIDC), Flyway, PostgreSQL 16 (H2 in tests), Jakarta Mail
(IMAP/SMTP), Stripe, jsoup. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, pricing/legal/SEO/PWA, SSE `ChatEventsController` |
| `service` | Conversation building (`ConversationService`, `IMTransformService`, `EmailBodyCleaner`), `ReplyService` (SMTP send), `ConversationKeyService`, list/chat services |
| `email` | IMAP import (`MailboxPollingService`, `EmailImportService`, `MimeMessageParser`), IDLE (`ImapIdleService`), Gmail OAuth, credential encryption |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team…) |
| `repository` | Spring Data repositories |
| `auth` | Security config, registration, login throttle, password reset, email verification, account settings, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), banners |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, lifecycle emails, admin metrics |

## Core model
- A **conversation** = all messages sharing a participant set, identified by
  `conversationKey` (`EmailThread.conversation_key`, V32). Multiple email
  threads merge into one timeline (`ChatConversation`).
- Bubbles: `SenderBubbleRun` → `BubbleMessage` (quoted replies stripped,
  consecutive same-side messages grouped).
- Multi-account: a user has N `MailAccount`s; `?mailbox=` scopes the view,
  absent = cross-account "Hub".
- Live delivery: `MailboxPollingService` / `ImapIdleService` publish
  `NewMailEvent` → `ChatEventStream` → SSE `/chats/events`.

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token
repo), CSRF **enabled** (exempt: `/h2-console/**`, `/billing/webhook`). Public
paths are listed in `auth/SecurityConfig`; everything else requires
authentication. Login: `POST /login` (`email`, `password`). **No token/bearer
auth exists yet** — see `INTERNAL_TODO.md`.

## Web vs. REST
Today there is **no REST API**. All pages are Thymeleaf controllers calling
services directly. Controllers still contain orchestration (e.g.
`ConversationController.send` picks the sending account, appends the signature,
calls `ReplyService` then `OutboundMessageService`); this must be extracted into
a service before a REST controller reuses it (INTERNAL_TODO #2).

## Schema
Flyway migrations in `src/main/resources/db/migration` (immutable once merged).
Tables: users, subscriptions, mail_accounts, email_threads, messages,
message_recipients, participants, attachments, saved_searches,
digest_email_preferences, teams, team_members, team_invites, auth_events,
password_reset_tokens, email_verifications, plan_change_events,
persistent_logins (remember-me).
