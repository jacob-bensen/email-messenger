# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Reverse-engineered 2026-10-05;
items marked TBD are not determinable from code.

> Deviation from the generic sync-routine assumptions: persistence is **Spring
> Data JPA + Flyway** (not jOOQ — there is no codegen step), and there is **no
> REST/JSON API yet**. All user-facing surface is server-rendered Thymeleaf.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, password reset, email verification, login throttling, `/account/*` settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: Free 3 mailboxes/500 threads, Pro 5/unlimited, Business unlimited), trial nudges |
| `email` | Mailbox integration: IMAP client, IMAP IDLE, Gmail OAuth, polling, MIME parsing, import into threads, credential encryption |
| `service` | IM transform: `ConversationKeyService` (participant-set key), `EmailBodyCleaner`, `IMTransformService`, `ReplyService`, chat view-model records |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, PWA, SEO, legal), `ChatEventStream` (SSE), list/chat services |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team` | Team invites and membership |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |
| `admin` | Revenue/retention metrics, `/admin/*` (admin-allow-listed) |

## Data

Flyway migrations `V1`–`V35` in `src/main/resources/db/migration` (immutable once
merged). PostgreSQL 16 in prod/local; H2 in tests. Core tables: `users`,
`subscriptions`, `mail_accounts`, `email_thread` (carries `conversation_key`,
owner), `message`, `participant`, `attachment` (`blob_ref` always null — bytes are
not stored), `saved_searches`, `teams`/`team_members`/`team_invites`,
`auth_events`, `plan_change_events`, token tables for reset/verify.

## Auth model

- Session cookie auth: form login (`/login`, optional remember-me) or Google OIDC
  (`/auth/google/start`). Everything not in the `SecurityConfig` permit-list is
  `authenticated()`.
- CSRF is **enabled** except for `/h2-console/**` and `/billing/webhook`.
- No token/bearer auth exists. A mobile client therefore cannot authenticate
  today (see `INTERNAL_TODO.md`).
- Controllers resolve the user via `userService.requireByEmail(principal.getName())`.

## Web vs. REST

There is no REST layer. The only `@RestController` is `BillingWebhookController`
(Stripe → server). One `@ResponseBody` endpoint exists: `POST /chats/refresh`
(returns `{"imported": n}`), used by the web UI. `GET /chats/events` is an SSE
stream used for live updates. See `docs/API.md` for the status of a future
mobile API and `docs/WEB.md` for pages.
