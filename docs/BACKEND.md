# BACKEND.md — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf UI;
**no JSON REST API exists yet** (see `docs/API.md`).

## Stack (as implemented)
JPA/Hibernate (`ddl-auto: validate`) — **not jOOQ**; Flyway (`src/main/resources/db/migration`, V1..V35);
PostgreSQL in prod/local, H2 in tests; Spring Security (form login, Google OIDC, remember-me, CSRF on);
Stripe for billing; Jakarta Mail (IMAP polling + IDLE, SMTP send); SSE for live updates.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Registration/login, Google OIDC, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (FREE 3 mailboxes/500 threads; PRO 5/unlimited; BUSINESS unlimited), banners, trial nudges |
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, … |
| `repository` | Spring Data repositories |
| `email` | IMAP client, polling (`MailboxPollingService`), IDLE (`ImapIdleService`), MIME parsing, import, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService` |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA, SEO), SSE `ChatEventStream` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |

## Core model
Conversations are keyed by a participant-set `conversationKey` (`/chats/{key}`), spanning one or more
`EmailThread`s (Message-ID / In-Reply-To / References). Messages are grouped into bubble runs by
`IMTransformService`. Owner = `User`; each `User` has many `MailAccount`s (IMAP password or Gmail OAuth).

## Auth model
Session cookie (`JSESSIONID`) + optional remember-me (30d). CSRF enforced except `/h2-console/**` and
`/billing/webhook` (Stripe signature). **No token/API-key auth for mobile clients exists** — required before any `/api/v1`.

## Web vs API
All logic lives in services; controllers only bind forms and populate models. A future REST layer must call
the same services (`ChatService`, `ConversationListService`, `ReplyService`, `MailAccountService`, `PlanLimitService`).
