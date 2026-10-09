# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-09. Items not verifiable from code are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no jOOQ dependency, no codegen), Flyway (V1..V35 in
`src/main/resources/db/migration`), PostgreSQL (prod/local), H2 (tests), Thymeleaf, Spring Security
(form login + optional Google OIDC), Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers, SSE stream, view services (`ChatService`, `ConversationListService`, `OutboundMessageService`), PWA/SEO |
| `service` | Conversation transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`) |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `email` | IMAP import, polling, IDLE, Gmail OAuth, credential encryption |
| `auth` | Security config, registration, password reset, email verification, Google OIDC, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |

## Auth model
Session-cookie form login (`/login`) or Google OIDC (`/auth/google/start`). CSRF is on, except
`/billing/webhook` (Stripe signature verified) and `/h2-console/**`. Everything not in the
permit-list in `SecurityConfig` requires authentication. **There is no token/API-key auth** — a mobile
client cannot authenticate today (see INTERNAL_TODO).

## Data
Core: `User`, `MailAccount` (encrypted creds), `EmailThread`, `Message`, `Participant`, `Attachment`
(`blob_ref` always null), `SavedSearch`, `Team*`, `Subscription`, `PlanChangeEvent`, `AuthEvent`.
Chats are keyed by participant-set `conversationKey`.

## Web vs API
All behaviour is exposed through server-rendered pages (see WEB.md). Controllers delegate to services,
so a future JSON API should reuse the same services (`ChatService`, `OutboundMessageService`, ...).

Prior planning docs live in `master/` (PLAN.md, BACKLOG.md, MASTER_ACTIONS.md, CHANGELOG.md).
