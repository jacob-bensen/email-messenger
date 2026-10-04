# BACKEND.md — architecture

Reverse-engineered 2026-10-04. Anything not verified in code is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate (not jOOQ)**, PostgreSQL 16
(H2 in tests), Flyway (35 migrations `V1..V35` in `src/main/resources/db/migration`),
Maven wrapper, Thymeleaf, Spring Security (form login + Google OIDC), Stripe,
Jakarta Mail (IMAP/SMTP), GreenMail + Testcontainers in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, legal, PWA, SEO), `ChatEventStream` (SSE), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService` (SMTP send) |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, login throttle, password reset, email verification, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |

## Data model
`User` → `MailAccount`(s) (IMAP password or Gmail OAuth, encrypted creds) →
`EmailThread` → `Message` (+`MessageRecipient`, `Attachment`, `Participant`).
Chats are keyed by `conversationKey` (participant set), merging threads.
Also `Subscription`, `PlanChangeEvent`, `Team*`, `SavedSearch`, `AuthEvent`, token tables.

## Auth model
Session cookie (form login `/login`, or Google OIDC via `/auth/google/start`).
CSRF enabled (ignored only for `/billing/webhook`, `/h2-console/**`). Everything
not on the public allowlist in `SecurityConfig` requires authentication.
**There is no token/Bearer auth and no JSON REST API yet** — see `API.md`.

## Web vs. API
All current endpoints are server-rendered web routes (`WEB.md`). The only
JSON/stream-ish endpoints: `POST /billing/webhook` (Stripe), `GET /chats/events`
(SSE), one `@ResponseBody` in `ConversationController` (`POST /chats/refresh`).
A mobile REST API must reuse `ChatService`/`ConversationService`/`ReplyService`
rather than the controllers.
