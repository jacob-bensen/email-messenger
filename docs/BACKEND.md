# BACKEND architecture (ConexusMail / email-messenger)

Reverse-engineered 2026-10-28 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (NOT jOOQ), Flyway
(`src/main/resources/db/migration`, V1..V35), PostgreSQL (H2 in tests),
Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, landing, PWA, SEO), SSE stream `ChatEventStream` |
| `service` | Conversation building, IM transform, body cleaning, `ReplyService` |
| `email` | IMAP/Gmail import, polling, IDLE, credential encryption |
| `auth` | Form login, Google OIDC, registration, verification, reset, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle schedulers, operator metrics |
| `domain`, `repository` | JPA entities / Spring Data repos |

## Auth model
Session cookie (JSESSIONID) via form login (`POST /login`, params `email`,
`password`, `remember-me`) or Google OIDC. CSRF is ON (exempt: `/h2-console/**`,
`/billing/webhook`). Everything not in the permitAll list in `SecurityConfig`
requires authentication. There is **no token/bearer auth** today (needed for
mobile; TBD).

## Domain
`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`
(`blob_ref` always null: not downloadable), `SavedSearch`, `Subscription`,
`Team*`. A *conversation* is identified by `conversationKey` (participant set)
and merges messages across threads.

## Web vs API
All HTML pages call services directly from controllers. The only JSON/SSE
endpoints are `POST /chats/refresh` and `GET /chats/events`. See `API.md`.
Other docs: `master/PLAN.md`, `master/MASTER_ACTIONS.md` (ops), `master/CHANGELOG.md`
(historical product changelog, 3.5k lines; root `CHANGELOG.md` is the sync-routine log).
