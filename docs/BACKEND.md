# BACKEND.md — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website + (currently) **no JSON API**. See `API.md` for the contract status.

## Stack as implemented
- Persistence: **Spring Data JPA / Hibernate** (`ddl-auto: validate`). jOOQ is
  **not used** (the generic routine assumed it; it does not apply here).
- Schema: Flyway, `src/main/resources/db/migration/V1..V35`. Immutable once merged.
- DB: PostgreSQL 16 (prod/local), H2 for tests.
- Build: Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, legal, PWA, SEO), SSE `ChatEventStream`, list/chat services |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records |
| `email` | IMAP/Gmail integration: `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `MailAccountService`, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, form login, Google OIDC, registration, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team` | Team invites/membership |
| `digest`, `activation`, `reengagement`, `admin` | Schedulers/emails/admin metrics |

## Core model
`participants` (deduped by email) → `messages` (+`message_recipients`,
`attachments`) → `email_threads`. Conversations in the UI are keyed by a
participant-set `conversationKey` (`ConversationKeyService`) that merges
multiple threads. Per-user ownership; `mail_accounts` hold encrypted
credentials (IMAP password or Gmail OAuth). Other tables: `users`,
`subscriptions`, `plan_change_events`, `persistent_logins`, `auth_events`,
`email_verifications`, `password_reset_tokens`, `teams`, `team_members`,
`team_invites`, `saved_searches`, `digest_email_preferences`.

## Auth model
Session cookie (form login + remember-me, Google OIDC optional via
`auth.google.*`). CSRF **enabled** except `/h2-console/**` and
`/billing/webhook`. Everything not on the public allow-list in
`SecurityConfig` requires authentication. There is **no token (bearer/JWT)
auth**, so a mobile client cannot authenticate today.

## Web vs. API
Controllers resolve the user via `UserService.requireByEmail(principal)` and
delegate to services; a future `/api/v1` layer must reuse the same services
(`ConversationListService`, `ChatService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, `PlanLimitService`). Known
exception: `ConversationController.send` contains orchestration
(new-vs-reply decision, signature append, recipient derivation) that should
move into a service before an API reuses it — see `INTERNAL_TODO.md`.

## Live updates
`MailboxPollingService` / `ImapIdleService` publish `NewMailEvent`;
`ChatEventStream` fans out to per-user SSE emitters at `GET /chats/events`.
