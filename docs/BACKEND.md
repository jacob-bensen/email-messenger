# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot, Flyway under `src/main/resources/db/migration`).
Sibling: `conexus-mail-mobile` (FRONTEND; currently only a README — no Flutter project yet).

## Deviations from the routine's assumed stack
- Persistence is **Spring Data JPA/Hibernate**, not jOOQ. There is no jOOQ codegen. Do not introduce jOOQ without a decision.
- Flyway has 35 migrations (`V1`…`V35`), immutable once merged. Tests run on H2; prod/dev on PostgreSQL 16.
- There is **no REST API for a mobile client yet**. The app is a server-rendered Thymeleaf SaaS ("ConexusMail"). See `API.md`.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, PWA, SEO, legal, saved searches), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Spring Security config, form login + Google OIDC, registration, verification, password reset, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | IMAP import (`JakartaImapClient`, `EmailImportService`), Gmail OAuth, IDLE (`ImapIdleService`), polling scheduler, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService`, view-model records |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `admin`, `activation`, `reengagement` | teams + invites, weekly digests, operator metrics, lifecycle emails |

## Schema overview (tables)
users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, thread_notes, teams, team_members, team_invites, email_verifications, password_reset_tokens, auth_events, digest_email_preferences, persistent_logins.

## Auth model
- Session cookie (JSESSIONID) + remember-me (`persistent_logins`). CSRF is on for everything except `/h2-console/**` and `/billing/webhook`.
- Login: form (`/login`) or Google OIDC (`/auth/google/start`). Login throttling via `LoginThrottleFilter`.
- Everything not on the permit-all list in `SecurityConfig` requires an authenticated session.
- No token (JWT/API-key) auth exists. A mobile client needs one — see `INTERNAL_TODO.md`.

## Web vs REST
All HTML endpoints delegate to services (`ChatService`, `MailAccountService`, `ReplyService`, …). Only two non-HTML JSON/stream endpoints exist for the browser: `POST /chats/refresh` and `GET /chats/events` (SSE). A future REST API must call the same services.
