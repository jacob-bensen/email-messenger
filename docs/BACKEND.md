# Backend architecture

Classification: BACKEND of the ConexusMail pair (sibling: `conexus-mail-mobile`, Flutter client — currently an empty shell).

> Deviations from the generic routine template, observed in code: persistence is **Spring Data JPA** (not jOOQ); there is **no REST API for the mobile client yet** (only server-rendered pages, one SSE stream and a Stripe webhook).

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Data JPA/Hibernate, Flyway (`src/main/resources/db/migration`, V1..V35), PostgreSQL 16 (H2 in tests), Jakarta Mail (IMAP/SMTP), Stripe, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account settings, login throttling/audit (`SecurityConfig`). |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/conversion nudges. |
| `email` | Mailbox connect (IMAP password, Gmail OAuth), IMAP polling + IDLE, RFC 822 parsing/import (`EmailImportService`), credential encryption. |
| `service` | Conversation building: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ChatService`-feeding view models, `ReplyService`. |
| `web` | Thymeleaf controllers, `ChatEventStream` (SSE), form objects, model advices. |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digests, admin metrics, lifecycle emails. |
| `domain` / `repository` | JPA entities / Spring Data repos. |

## Schema overview (Flyway)
Core: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`, `email_threads`, `messages`, `message_recipients`, `attachments` (`blob_ref` always null today). Conversations are keyed by a participant-set `conversationKey`. Other: `teams`, `team_members`, `team_invites`, `saved_searches`, `digest_email_preferences`, `email_verifications`, `password_reset_tokens`, `auth_events`, `persistent_logins`. (`thread_notes` dropped in V33.) Latest migration: `V35__mail_account_signature.sql`. Migrations are immutable once merged.

## Auth model
Session-cookie auth (form login, remember-me, optional Google OIDC). CSRF is enabled except `/h2-console/**` and `/billing/webhook`. Everything is `authenticated()` except the public allow-list in `SecurityConfig` (landing, pricing, legal, login/register, password reset, PWA assets, health). No token/Bearer auth exists — **a mobile client cannot authenticate yet** (see INTERNAL_TODO).

## Web ↔ API relationship
All current endpoints are Thymeleaf/HTML controllers calling services (`ChatService`, `ReplyService`, `MailAccountService`, `BillingService`, …). Future JSON endpoints must call the same services; see `API.md`.

## Live delivery
`MailboxPollingService` publishes `NewMailEvent`; `ChatEventStream` fans out to open SSE emitters at `/chats/events`; IMAP IDLE (`ImapIdleService`) is enabled for paid tiers behind `mailbox.idle.enabled`.
