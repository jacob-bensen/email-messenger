# BACKEND.md — architecture

Reverse-engineered 2026-10-03 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ), Flyway (V1–V35 in
`src/main/resources/db/migration`), PostgreSQL (prod/local), H2 (tests),
Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Gmail OAuth.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, remember-me (30d, JDBC token repo), login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial/win-back schedulers |
| `email` | Mailbox connect (IMAP password + Gmail OAuth), polling import, IMAP IDLE, MIME parsing, credential encryption |
| `service` | IM transform: conversation keys, bubble runs, quoted-reply stripping, reply/send |
| `domain` / `repository` | JPA entities (users, subscriptions, mail_accounts, email_threads, messages, participants, attachments, saved_searches, teams, team_invites, auth_events, …) and Spring Data repos |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, landing, legal, PWA, SEO), SSE `ChatEventStream` |
| `team`, `digest`, `reengagement`, `activation`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,
`password`, `remember-me`) or Google OIDC. CSRF **enabled** (exempt:
`/h2-console/**`, `/billing/webhook`). Everything not on the permitAll list
in `auth/SecurityConfig.java` requires authentication. There is **no
token/bearer auth** — nothing suitable for a mobile client yet.

## Web vs. API
There is no REST API. The only non-HTML endpoints are the SSE stream
`/chats/events`, the Stripe webhook, and PWA/SEO assets. Controllers resolve
the user via `UserService.requireByEmail(principal)` and delegate to services
(`ConversationListService`, `ChatService`, `OutboundMessageService`,
`MailAccountService`, …). A future `/api/v1` layer must reuse these services.

## Data flow
Poll/IDLE → `MailboxPollingService` → `EmailImportService` (parse, thread,
conversationKey) → `NewMailEvent` → `ChatEventStream` (SSE) → browser morphs
rail/pane. Send: `/chats/{key}/send` → `OutboundMessageService`/`ReplyService` → SMTP.
