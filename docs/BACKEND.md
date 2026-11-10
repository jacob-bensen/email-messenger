# BACKEND.md — architecture

Reverse-engineered 2026-11-10 from the code. Unknowns are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, **Spring Data JPA (Hibernate)** — *not jOOQ*,
PostgreSQL (H2 in tests), Flyway (`V1`..`V35` in `src/main/resources/db/migration`), Maven wrapper.
Stripe for billing, Gmail OAuth + IMAP/SMTP for mail, Jakarta Mail for parsing.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `email` | IMAP/IDLE clients, import, polling scheduler, Gmail OAuth, `MimeMessageParser`, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `web` | Thymeleaf controllers + `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `auth` | form login + Google OIDC, registration, email verification, password reset, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digests, lifecycle emails, admin metrics |

## Core model
A **conversation** is keyed by `conversationKey` (hash of the participant set, `ConversationKeyService`), scoped per
mailbox owner address. A conversation merges several `EmailThread`s (Message-ID / In-Reply-To / References chains).
Messages render as bubble runs (consecutive same-sender grouped, quoted replies stripped).

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token repo). CSRF **on** except
`/billing/webhook` and `/h2-console/**`. Everything not whitelisted in `SecurityConfig` requires authentication.
**There is no token/bearer auth and no JSON API for a mobile client yet** (see `API.md`, `INTERNAL_TODO.md`).

## Web ↔ REST relationship
Today the web layer is the only client. Controllers delegate to services (`ChatService`, `MailAccountService`,
`OutboundMessageService`, `BillingService`, …). A mobile API must be added as thin `/api/v1` controllers calling
**the same services** — do not copy logic out of the Thymeleaf controllers (some orchestration, e.g. mailbox
selection/scope in `ConversationController`, still lives in the controller and should be extracted first).

## Live delivery
IMAP IDLE (`ImapIdleService`, paid tiers) and polling publish `NewMailEvent`; `ChatEventStream` fans out over
SSE at `GET /chats/events`. Web Push is a roadmap item (CLAUDE.md).
