# BACKEND.md — architecture

Last audited: 2026-10-28 (reverse-engineered from code; unknowns marked TBD).

Repo role: **BACKEND** (Spring Boot 3 / Java 21 / Maven / Flyway / PostgreSQL). Product name: ConexusMail.
Sibling: `conexus-mail-mobile` (Flutter client, currently an empty scaffold).

> Deviation from the generic stack: persistence is **Spring Data JPA/Hibernate** (`ddl-auto: validate`), **not jOOQ**.
> There is no codegen step. Do not introduce jOOQ without an owner decision.

## Package layout (`com.emailmessenger`)

| Package | Responsibility |
|---|---|
| `auth` | Form login, Google OIDC, registration, email verification, password reset, login throttling, account settings, `UserService` |
| `billing` | Stripe checkout/portal/webhook gateways, `Plan` limits (`PlanLimits`, `PlanLimitService`), trial nudges, billing banners |
| `email` | IMAP/Gmail-OAuth integration: `MailAccountService`, `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, credential encryption |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), view-model records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, …) |
| `web` | Thymeleaf controllers + view services (`ConversationController`, `ChatService`, `ConversationListService`, `MailboxController`, `OutboundMessageService`, SSE `ChatEventStream`, PWA/SEO controllers) |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, operator metrics (`/admin/revenue`) |

## Data model (Flyway, V1–V35, `src/main/resources/db/migration`)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads`, `messages`, `message_recipients`, `participants`, `attachments`, `saved_searches`, `teams`, `team_members`, `team_invites`, `thread_notes`, `auth_events`, `email_verifications`, `password_reset_tokens`, `persistent_logins`, `digest_email_preferences`.
Key concept: a **conversation** = participant-set `conversationKey` (string) grouping messages across `email_threads`. Per-user scoping everywhere (`owner`).
Plans: `FREE`, `PRO`, `BUSINESS`; caps in `billing/PlanLimits` (mailboxes, threads, saved searches).
Attachments: metadata only; `blob_ref` is always null (bytes are not stored/downloadable).

## Auth model (current)
- Session cookie (`JSESSIONID`) via form login (`/login`, params `email`,`password`) or Google OIDC; remember-me (30 days, JDBC token repo).
- CSRF **enabled** (exempt: `/h2-console/**`, `/billing/webhook`). Everything not on the permit-list requires authentication.
- **No token/bearer authentication exists.** A mobile client cannot authenticate today (see `INTERNAL_TODO.md` S1).

## Web ↔ API relationship
There is **no REST API**. All user-facing behavior is server-rendered Thymeleaf (`docs/WEB.md`). The only JSON/streaming endpoints are `POST /chats/refresh` (JSON) and `GET /chats/events` (SSE), both session+CSRF bound. Planned approach (TBD): add `/api/v1/**` controllers in a new `api` package that call the same services the web controllers use (`ConversationListService`, `ChatService`, `ReplyService`/`OutboundMessageService`, `MailAccountService`, `BillingService`), with bearer-token auth in a second `SecurityFilterChain`.
Caveat: several services are package-private in `web` (`ChatService`, `OutboundMessageService`) and some orchestration lives in `ConversationController` (account scoping, signature append, recipient parsing, mark-read ordering). That logic must be extracted into services before an API can share it (INTERNAL_TODO L1).

## Commands
- Compile/test: `mvn -q -B verify` (system Maven works; `./mvnw` needs to download Maven and may fail offline).
- Run locally: `JDBC_DATABASE_PASSWORD=... mvn spring-boot:run -Dspring-boot.run.profiles=local`.
- Tests use H2 (`src/test/resources/application.yml`); Testcontainers + GreenMail for integration tests.
