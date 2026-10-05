# Backend architecture

Classification: this repo is the BACKEND (Spring Boot, Flyway migrations, `pom.xml`).
Sibling: `conexus-mail-mobile` (Flutter client; currently an empty scaffold).

Deviations from the generic target stack: persistence is **Spring Data JPA/Hibernate**
(`ddl-auto: validate`), **not jOOQ**; there is no codegen step. Java 21, Maven, Flyway
(V1..V35, immutable), PostgreSQL in prod, H2 in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | form login, Google OIDC, registration, email verification, password reset, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | mailbox connect (IMAP password / Gmail OAuth), polling, IMAP IDLE, MIME parsing, import |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService` |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, landing, legal, PWA), SSE `ChatEventStream` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, admin metrics |

## Auth model
Session cookie (Spring Security form login or Google OIDC), CSRF enabled (ignored only for
`/billing/webhook` and `/h2-console/**`). Everything not on the permitAll list in
`SecurityConfig` requires authentication. **There is no token/bearer auth for a mobile client yet (TBD).**

## Web vs. API
The app is server-rendered. The only JSON/stream endpoints are `POST /chats/refresh`,
`GET /chats/events` (SSE) and `POST /billing/webhook`. See `API.md`. Controllers delegate to
services in `service/`, `web/*Service`, `email/`, `billing/`.

## Schema overview
See `src/main/resources/db/migration`. Core tables: users, mail_account, email_thread,
message, message_recipient, participant, attachment (`blob_ref` always null today),
subscription, plan_change_event, team*, saved_search, tokens, auth_event.
