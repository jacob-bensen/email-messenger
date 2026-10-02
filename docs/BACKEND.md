# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). It serves the server-rendered
website (Thymeleaf) and owns all persistence and business rules. As of
2026-10-02 it exposes **no REST API for the mobile client** (see `API.md`).

> Deviation from the generic routine template: persistence is **Spring Data
> JPA + Flyway** (no jOOQ, no codegen). Verified from `pom.xml` and the
> `repository/` package.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace), `ChatService`, `ConversationListService`, `OutboundMessageService`, mailbox connect controllers, SSE `ChatEventStream`, PWA/SEO/legal controllers |
| `service` | Email→IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records (`BubbleRun`, `ChatConversation`, …) |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), Gmail OAuth, polling (`MailboxPollingService/Scheduler`), RFC 822 parsing, import (`EmailImportService`) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security config, form + Google OIDC login, registration, password reset, email verification, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |

## Persistence

PostgreSQL 16 (prod/local), H2 in tests. Flyway migrations `V1`…`V35` in
`src/main/resources/db/migration`; immutable once merged. Conversations are
keyed by participant-set `conversationKey` (V32).

## Auth model

Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token
repo). Form login at `POST /login` (params `email`, `password`) or Google OIDC
(only when `GOOGLE_CLIENT_ID` is set). **CSRF is on** for everything except
`/h2-console/**` and `/billing/webhook`. Everything not in the permit-all list
in `SecurityConfig` requires authentication. There is **no token/API-key
auth** yet — a mobile client cannot authenticate without it (see
`INTERNAL_TODO.md`).

## Web vs. API

Controllers in `web/` return Thymeleaf views and delegate to services
(`ChatService`, `ConversationListService`, `ReplyService`, …). Only two
handlers return JSON/stream bodies today: `POST /chats/refresh`
(`{"imported": n}`) and the SSE stream `GET /chats/events`. A future REST layer
must call the same services — not re-implement controller logic (several
controllers currently contain request-shaping and plan-limit logic; see
`INTERNAL_TODO.md`).

## Commands

See `CLAUDE.md`.
