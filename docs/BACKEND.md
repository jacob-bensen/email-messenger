# Backend architecture

Reverse-engineered from the code on 2026-11-01. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ — there is no
jOOQ codegen), PostgreSQL 16 (H2 in tests), Flyway (`V1`–`V35`), Maven wrapper,
Thymeleaf, Spring Security, Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP),
jsoup, GreenMail + Testcontainers in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plans & limits (`PlanLimitService`), trial/billing banners |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IMAP IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `web` | Thymeleaf controllers, chats workspace, mailbox mgmt, SSE, PWA, SEO, legal |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin revenue/retention metrics |

## Data model (tables via Flyway)
users (+ google subject, verification, appearance/mail prefs), subscriptions
(+ billing period, cancellation reason, win-back/trial tracking),
plan_change_events, mail_accounts (IMAP password or Gmail OAuth; last-seen
inbox/sent UIDs; polling policy; signature), email_thread (owner,
`conversation_key` = participant-set key), message (+ outbound flag),
message_recipient, participant (deduped by normalized address), attachment
(`blob_ref` always null — bytes not stored), saved_search, digest preferences,
teams/team_members/team_invites, password_reset_tokens,
email_verification_tokens, auth_events, persistent_logins (remember-me).

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, params `email`,
`password`) or Google OIDC; remember-me cookie (30 days, JDBC token repo).
CSRF is ON for everything except `/billing/webhook` and `/h2-console/**`.
`anyRequest().authenticated()`; public routes are listed in `SecurityConfig`.
**No token/Bearer auth exists** — a mobile client cannot authenticate today.

## Web vs REST
The backend is a server-rendered web app. There is **no REST/JSON API for a
mobile client** (see `API.md`). Only JSON-ish endpoints: `POST /chats/refresh`,
`POST /billing/webhook`; plus SSE `/chats/events`. Controllers delegate to the
service layer (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailAccountService`, …); a future `/api/v1` controller layer must reuse those
services, not copy controller logic (several controllers, e.g.
`ConversationController`, still hold some selection/scoping helpers
— see `INTERNAL_TODO.md`).
