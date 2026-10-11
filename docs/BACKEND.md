# Backend architecture

Reverse-engineered from the code on 2026-10-11. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3.5.x, Spring MVC + Thymeleaf, Spring Data **JPA** (not jOOQ), Flyway
(35 migrations, `src/main/resources/db/migration`), PostgreSQL (prod/local), H2 (tests), Maven wrapper.
No jOOQ codegen exists.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | form login, Google OIDC, registration, password reset, email verification, account pages, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP/Gmail-OAuth mailbox connect, polling import, IMAP IDLE, MIME parsing, credential encryption |
| `service` | conversation building (`ConversationService`, `ChatConversation`), IM transform, `ReplyService` |
| `web` | Thymeleaf controllers (chats, mailboxes, pricing/landing, PWA, SEO, saved searches), SSE stream |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema overview
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages,
message_recipients, participants, attachments (`blob_ref` always null today), saved_searches,
teams, team_members, team_invites, auth_events, password_reset_tokens, email_verifications,
digest_email_preferences, persistent_logins. Conversations are keyed by participant-set `conversationKey`.

## Auth model
Session-cookie auth (form login + optional Google OIDC), remember-me. CSRF is ON everywhere except
`/h2-console/**` and `/billing/webhook`. All routes except an allow-list (see `SecurityConfig`) require
authentication. **There is no token/API-key auth, so a mobile client cannot authenticate today.**

## Web vs. REST
The app is server-rendered. Only three non-HTML endpoints exist (`POST /chats/refresh` JSON,
`GET /chats/events` SSE, `POST /billing/webhook`). There is no `/api/**` surface. Controllers
delegate to services (`ConversationService`, `ReplyService`, `MailAccountService`, ...); a future
`/api/v1` layer must reuse those services.
