# Backend architecture

Spring Boot 3 / Java 21 app, product name ConexusMail. Maven, Spring Data JPA
(**not jOOQ**), Flyway (`src/main/resources/db/migration`, V1–V12 plus later),
PostgreSQL in prod, H2 in tests. Server-rendered Thymeleaf; no SPA.

## Packages (`com.emailmessenger`)

- `domain` — JPA entities: User, MailAccount, EmailThread, Message, Participant,
  Attachment, MessageRecipient, SavedSearch, Subscription, Team/TeamMember/TeamInvite,
  PlanChangeEvent, AuthEvent, token entities.
- `repository` — Spring Data repositories.
- `email` — IMAP/Gmail integration: `EmailImportService`, `MimeMessageParser`,
  `MailboxPollingService`/`Scheduler`, `ImapIdleService`, `GmailOAuthClient`,
  `CredentialEncryptor`, `MailAccountService`.
- `service` — IM transform and conversation view models: `IMTransformService`,
  `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`, `ReplyService`.
- `web` — Thymeleaf controllers and web-facing services (`ChatService`,
  `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, onboarding, PWA, SEO, SSE `ChatEventStream`).
- `auth` — Spring Security config, form login, Google OIDC (only when `GOOGLE_CLIENT_ID` set), password reset, email verification, login throttling.
- `billing` — Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial conversion.
- `team`, `digest`, `reengagement`, `activation`, `admin` — teams, digest emails, win-back, activation nudges, operator revenue/retention pages.

## Auth model

Form login (`email`/`password`), remember-me (30 days, persistent_logins table),
optional Google OIDC. CSRF on except `/billing/webhook` and `/h2-console/**`.
Everything not in the `SecurityConfig` permit list requires authentication.
No bearer-token auth exists.

## Key concept

Conversations are keyed by the participant set (`conversationKey`), not just the
`Message-ID` chain; `/chats/{key}` merges threads for the same participants.

## Related

REST API: `API.md` (none yet). Pages: `WEB.md`. Sibling repo: `conexus-mail-mobile`.
Legacy planning notes live in `master/` (PLAN, BACKLOG, MASTER_ACTIONS, 500KB CHANGELOG) — a stray directory name, see `INTERNAL_TODO.md`.
