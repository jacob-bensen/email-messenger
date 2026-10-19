# Backend architecture

Reverse-engineered 2026-10-19 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ), PostgreSQL 16 (H2 in tests),
Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security,
Stripe, Jakarta Mail (IMAP/SMTP), Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, PWA, SEO, legal), `ChatEventStream` (SSE) |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits, trial nudges |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | Conversation building, IM transform (bubbles), reply/outbound sending |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |

## Schema overview
Core: `users`, `subscriptions`, `mail_accounts`, `email_thread` (carries `conversation_key`),
`message`, `message_recipient`, `participant`, `attachment` (`blob_ref` always null today),
`saved_searches`, teams/invites, auth_events, plan_change_events.

## Auth model
Session cookie (`JSESSIONID`) + 30-day remember-me, form login at `/login` (param `email`,
`password`), optional Google OIDC. **CSRF is enabled** (ignored only for `/billing/webhook`).
Everything not on the permit-list in `SecurityConfig` requires authentication.
There is **no token/bearer auth** and no `/api/**` surface.

## Web vs REST
All user-facing behavior is server-rendered Thymeleaf. The only JSON/stream endpoints are
`POST /chats/refresh` (JSON) and `GET /chats/events` (SSE). A mobile REST API (`/api/v1`) is
planned but not implemented; see `docs/API.md`.
