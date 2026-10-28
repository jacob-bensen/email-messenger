# BACKEND

Spring Boot 3.5 / Java 21 monolith ("ConexusMail", repo `email-messenger`). Server-rendered
Thymeleaf website + form/session auth. **No JSON REST API exists yet** (see `API.md`).

Deviations from the generic target stack: persistence is **Spring Data JPA** (not jOOQ);
Flyway migrations live in `src/main/resources/db/migration` (V1..V35 at time of writing).
PostgreSQL in prod/dev, H2 in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService` (SMTP send), view-model records |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, `MailAccountService`, credential encryption |
| `auth` | Spring Security config, form login, Google OIDC, registration, verification, password reset, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digest emails, lifecycle schedulers, operator metrics |
| `domain`, `repository` | JPA entities / Spring Data repos |

## Schema overview
users, subscriptions, plan_change_events, mail_accounts, email_threads, messages,
message_recipients, participants, attachments (`blob_ref` always null — bytes not stored),
saved_searches, thread_notes, teams, team_members, team_invites, digest_email_preferences,
email_verifications, password_reset_tokens, auth_events, persistent_logins.
Core concept: a **conversation** is keyed by `conversationKey` (participant set), spanning
multiple `email_threads`.

## Auth model
Spring Security session cookie (form login `/login`, Google OIDC, remember-me), CSRF enabled
(ignored only for `/h2-console/**`, `/billing/webhook`). Principal name = user email; controllers
call `UserService.requireByEmail`. **No token/bearer auth for a mobile client yet.**

## Web vs API
All current controllers are Thymeleaf. Business logic is in services (`ChatService`,
`ReplyService`, `OutboundMessageService`, `MailAccountService`, …). Some orchestration is still in
`ConversationController` (recipient parsing, sender selection, signature append, send+record
sequence in `createMessage`/`send`) and must move into a service before a REST controller can reuse it.
See `INTERNAL_TODO.md`.
