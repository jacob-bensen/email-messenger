# BACKEND architecture

Spring Boot 3.5 / Java 21 / Maven monolith. **Persistence is Spring Data JPA (Hibernate) + Flyway — not jOOQ.**
PostgreSQL in prod/local, H2 in tests. Server-rendered Thymeleaf website; no JSON REST API exists yet
(see `docs/API.md` and `INTERNAL_TODO.md`). Root package `com.emailmessenger`.

## Modules (packages under `src/main/java/com/emailmessenger`)

| Package | Role |
|---|---|
| `auth` | Spring Security config, form login + Google OIDC, registration, email verification, password reset, account settings, login throttling, auth-event audit |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitKind`), subscription lifecycle |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, SavedSearch, Team*, Subscription, ...) |
| `repository` | Spring Data repositories |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models (`ConversationService`, `ChatConversation`, `BubbleRun`), conversation keys, `ReplyService` (SMTP send) |
| `web` | Thymeleaf controllers, SSE (`ChatEventStream`), PWA/SEO controllers, advice |
| `team` | Team invites |
| `admin`, `activation`, `reengagement`, `digest` | Operator revenue dashboard, win-back, activation nudges, digest emails |

## Data model (Flyway `src/main/resources/db/migration`, V1..V35, immutable once merged)

Core tables: `users`, `mail_accounts`, `email_threads`, `messages`, `message_recipients`, `participants`,
`attachments` (`blob_ref` is always null today — bytes not stored), `saved_searches`, `thread_notes`,
`subscriptions`, `plan_change_events`, `teams`, `team_members`, `team_invites`, `digest_email_preferences`,
`auth_events`, `email_verifications`, `password_reset_tokens`, `persistent_logins` (remember-me).
Chats are keyed by a participant-set `conversationKey` (`ConversationKeyService`).

## Auth model

- Browser session (JSESSIONID) via form login (`email`/`password`) or Google OIDC (only when `GOOGLE_CLIENT_ID` is set);
  30-day remember-me (persistent token repo).
- CSRF is ON for everything except `/h2-console/**` and `/billing/webhook` (Stripe signature).
- Everything not whitelisted in `SecurityConfig` requires an authenticated session.
- **There is no token/bearer auth for a mobile client yet.**

## Web vs. API

All current endpoints are Thymeleaf/HTML (plus one SSE stream, one Stripe webhook, PWA/SEO assets).
Business logic lives in `service`/`email`/`billing`/`auth` services; controllers are thin. A future `/api/v1`
layer must call these same services (see `INTERNAL_TODO.md` item 1).

## Build / run
See `CLAUDE.md`. Historical per-iteration notes: `master/CHANGELOG.md`, `master/PLAN.md`, `master/BACKLOG.md`.
