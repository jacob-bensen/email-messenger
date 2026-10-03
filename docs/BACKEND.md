# Backend architecture (ConexusMail / email-messenger)

Reverse-engineered 2026-10-03 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate** (`ddl-auto: validate`; **not jOOQ**),
PostgreSQL (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf,
Spring Security (form login + Google OIDC + remember-me), Stripe, Jakarta Mail (IMAP/SMTP), Maven.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, saved searches, PWA, SEO, legal, pricing) and web-only services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream`) |
| `service` | Conversation view models (`ChatConversation`, `ConversationListItem`, `BubbleRun`...), `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP), `ConversationKeyService` |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, login throttling, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5, Business unlimited) |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digest emails, operator metrics (`/admin/**`, allowlist), lifecycle schedulers |

## Schema overview
users, participants, email_threads (+`conversation_key`), messages, message_recipients, attachments
(`blob_ref` always null today), mail_accounts (encrypted creds, polling state, signature),
subscriptions, plan_change_events, saved_searches, teams/team_members/team_invites, thread_notes,
auth_events, email_verifications, password_reset_tokens, persistent_logins, digest_email_preferences.

## Auth model
Session cookie (`JSESSIONID`) + CSRF on all POSTs (exempt: `/billing/webhook`, `/h2-console/**`),
30-day remember-me, login throttle filter. Everything not whitelisted in `SecurityConfig` requires auth.
**There is no token/bearer auth; a mobile client cannot authenticate today (TBD design: see docs/API.md).**

## Web vs REST
There is **no REST API**. The only JSON/non-HTML endpoints are `POST /chats/refresh` (JSON `{"imported":n}`),
`GET /chats/events` (SSE) and `POST /billing/webhook`. All business logic sits in services, but
`ConversationController` still orchestrates send logic (recipient resolution, signature append,
reply-vs-new-thread) inline — this must be extracted into a service before a JSON API can share it.
See INTERNAL_TODO.md.
