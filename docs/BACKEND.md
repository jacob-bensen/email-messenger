# BACKEND.md — architecture

Classification: this repo is the BACKEND (pom.xml, Spring annotations, Flyway migrations V1–V35). The sibling `conexus-mail-mobile` is the FRONTEND (Flutter; currently only a README).

## Stack (as implemented)
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ — no codegen step), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Gmail OAuth. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, SEO, PWA, legal |
| `auth` | Security config, registration/login, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `email` | IMAP/Gmail import, polling, IMAP IDLE, MIME parsing |
| `service` | Chat/conversation view models, IM transform, reply/send, body cleaning |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams, digests, operator metrics, lifecycle emails |

## Schema overview
Flyway `src/main/resources/db/migration`, V1–V35, immutable once merged. Core tables: users, mail_account, email_thread, message, message_recipient, participant, attachment (`blob_ref` always null today), saved_search, subscription, plan_change_event, team/team_member/team_invite, auth_event, token tables.

## Auth model
Session-cookie auth: form login (`/login`, params `email`/`password`, remember-me 30d) and optional Google OIDC. **CSRF is on** (exempt: `/h2-console/**`, `/billing/webhook`). Everything not on the public allow-list in `SecurityConfig` requires authentication. There is **no token/API-key auth** yet.

## Web vs REST
There is currently no REST API. The only JSON/stream endpoints are `POST /chats/refresh` (returns `{"imported":n}`) and `GET /chats/events` (SSE), both used by the web UI and session+CSRF protected. All other pages return HTML. Controllers resolve the user via `UserService.requireByEmail(principal)` and delegate to services (`ConversationListService`, `ChatService`, `ReplyService`, `MailAccountService`, ...). A future mobile API must reuse these services.

See `docs/API.md` (contract) and `docs/WEB.md` (pages). Pre-existing planning docs live in `master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS).
