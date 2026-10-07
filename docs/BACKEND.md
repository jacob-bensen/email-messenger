# Backend architecture

Reverse-engineered from the code on 2026-10-07. Unknowns are marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3, Thymeleaf, **Spring Data JPA** (not jOOQ), PostgreSQL
(H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V34),
Spring Security, Stripe SDK, Jakarta Mail (IMAP/SMTP). No jOOQ codegen exists.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, login throttle, account settings, remember-me |
| `billing` | Free/Pro/Business plans, Stripe checkout/portal/webhook, plan limits (`PlanLimitKind`: THREAD_COUNT, MAILBOX_COUNT, SAVED_SEARCH_COUNT), banners |
| `email` | Mailbox connect (IMAP password / Gmail OAuth), polling import, IMAP IDLE (paid tiers), MIME parsing, credential encryption |
| `service` | Conversation building: `ConversationKeyService` (participant-set key), `IMTransformService`/`EmailBodyCleaner` (quote stripping, bubble runs), `ReplyService` |
| `web` | Thymeleaf controllers, `ChatService`, `ConversationListService`, SSE `ChatEventStream`, PWA, SEO, legal |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams+invites, weekly digests, lifecycle emails, admin revenue/retention metrics |

## Schema overview
users, persistent_logins (remember-me), subscriptions, plan_change_events,
mail_accounts (auth_type, polling policy, last-seen UIDs, signature),
email_threads (owner, `conversation_key`, unread), messages (+`outbound` flag),
message_recipients, participants, attachments (`blob_ref` always null today),
saved_searches, digest_email_preferences, password_reset_tokens,
email_verifications, auth_events, teams, team_members, team_invites.

## Auth model
Server-side session (`JSESSIONID`) via form login (`/login`, params `email`,
`password`) or Google OIDC; 30-day persistent remember-me. **CSRF is enabled**
for everything except `/h2-console/**` and `/billing/webhook`. Everything is
authenticated by default except the public allowlist in `SecurityConfig`.
There is **no token/JWT auth** and **no JSON API for a mobile client** yet.

## Web vs REST
All current endpoints are server-rendered web endpoints (see `WEB.md`). Only
JSON/non-HTML responses: `POST /chats/refresh` (`{"imported":n}`),
`GET /chats/events` (SSE), `POST /billing/webhook` (Stripe), PWA/SEO assets.
A future mobile API must call the same services (`ChatService`,
`ConversationListService`, `ReplyService`/`OutboundMessageService`,
`MailAccountService`, `BillingService`) — see `API.md` §Proposed.
