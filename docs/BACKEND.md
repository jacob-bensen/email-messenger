# BACKEND.md — architecture (reverse-engineered 2026-10-16)

ConexusMail backend: Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (not
jOOQ), PostgreSQL 16 (H2 in tests), Flyway, Maven, Thymeleaf. It serves the
public website AND the authenticated web app. **There is no REST/JSON API yet**
(see `API.md`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/legal/SEO/PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service/` | Email→chat transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), view records (`ChatConversation`, `BubbleRun`, `BubbleMessage`, `ChatMember`, `ConversationListItem`) |
| `domain/` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `repository/` | Spring Data repositories |
| `email/` | IMAP/Gmail import: `EmailImportService`, `MimeMessageParser`, `MailboxPollingService`, `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `auth/` | `SecurityConfig`, form login + Google OIDC, password reset, email verification, login throttle, audit events |
| `billing/` | Stripe checkout/portal/webhook, `PlanLimitService` (Free/Pro/Business caps) |
| `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` | Teams/invites, digests, lifecycle emails (schedulers off unless `*.enabled`), operator metrics at `/admin/**` |

## Core model
`conversationKey` (participant-set key, `ConversationKeyService`) groups
`EmailThread`s into one chat. A chat = merged timeline of messages across the
user's threads. Messages carry `outbound` (V30). `MailAccount` holds encrypted
IMAP/Gmail-OAuth credentials, UID cursors (`last_seen_uid`, `last_seen_sent_uid`),
polling policy and an optional signature (V35).

## Schema
Flyway `src/main/resources/db/migration`, V1–V35 (immutable once merged). Entities
validated with `ddl-auto: validate`.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`,
`password`) or Google OIDC; remember-me (30 days, persistent token table); CSRF
**enabled** (only `/h2-console/**` and `/billing/webhook` exempt). Everything
except an explicit permit-list requires authentication. No bearer/JWT/API-token
support exists. `/admin/**` gated by `admin.emails` allowlist (404 for others).

## Web vs. API
Only Thymeleaf exists. Several controllers hold business logic that a REST layer
would need to share (send flow in `ConversationController`: recipient parsing,
sender-account choice, signature, `recordNewEmail`) — tracked in `INTERNAL_TODO.md`.

## Live updates
`MailboxPollingService` publishes `NewMailEvent`; `ChatEventsController`
streams them at `GET /chats/events` (SSE). IMAP IDLE for paid plans.

## Commands
See `CLAUDE.md`. Tests: `./mvnw test` (H2 + GreenMail).
