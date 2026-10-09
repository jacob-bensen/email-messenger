# BACKEND.md — architecture

Reverse-engineered from code on 2026-10-09. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (`ddl-auto: validate`; **not jOOQ**),
PostgreSQL (prod/local), H2 (tests), Flyway (`V1`–`V35`), Thymeleaf, Spring Security,
Stripe (stripe-java), Jakarta Mail (IMAP/SMTP), GreenMail + Testcontainers in tests, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing/landing, legal, SEO, PWA), SSE stream, view services |
| `auth` | Security config, form login + Google OIDC, registration, password reset, email verification, account settings, login throttling/audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), trial nudges |
| `email` | Mailbox connect, IMAP import/polling/IDLE, MIME parsing, credential encryption, Gmail OAuth |
| `service` | Conversation building: threading, `conversationKey`, IM transform, body cleaning, reply sending |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team` | Teams + invites |
| `admin` | Admin revenue/funnel/churn metrics (`/admin/**`) |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |

## Data model (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`, `email_threads`
(has `conversation_key` = participant-set key, V32), `messages` (outbound flag V30),
`message_recipients`, `attachments` (`blob_ref` always null: bytes not stored), `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`,
`teams`, `team_members`, `team_invites`, `persistent_logins`. (`thread_notes` created V25, dropped V33.)

## Auth model
Session-cookie based (form login at `/login`, optional Google OIDC when `GOOGLE_CLIENT_ID/SECRET`
set), remember-me (`persistent_logins`), **CSRF enabled** except `/h2-console/**` and
`/billing/webhook`. Everything not in the public allow-list (`SecurityConfig`) requires
authentication. **There is no token (JWT/API key) auth — a mobile client cannot authenticate today.**

## Live updates
`MailboxPollingService` publishes `NewMailEvent(userId, importedCount)`; `ChatEventStream` fans out
as SSE `new-mail`. IMAP IDLE (`ImapIdleService`) for paid plans behind `mailbox.idle.enabled`.

## Web ↔ API relationship
Today the web UI is the only client. Controllers delegate to services (`ChatService`,
`ConversationListService`, `ReplyService`/`OutboundMessageService`, `MailAccountService`, …).
A REST API for mobile does **not exist** (see `API.md`); when added it must reuse these services.
