# BACKEND.md — ConexusMail server architecture

Reverse-engineered 2026-10-13 from the code. Unknowns are marked **TBD**.

> **Stack deviation from the generic target:** persistence is **Spring Data JPA /
> Hibernate** (not jOOQ). Migrations are Flyway (`V1`–`V35`). There is no jOOQ
> codegen step.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Security, JPA, Flyway, PostgreSQL 16
(prod/local) / H2 (tests), Jakarta Mail (IMAP/SMTP), Stripe, Maven wrapper.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `LegalController`, `PwaController`, `SeoController`, `ChatEventsController` SSE) and the services they use (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`, `SavedSearchService`) |
| `service` | Conversation model + IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records `ConversationListItem`, `ChatConversation`, `BubbleRun`, `BubbleMessage`, `ChatMember` |
| `email` | Mailbox integration: IMAP import/poll (`MailboxPollingService`, `MailboxPollingScheduler`), IDLE (`ImapIdleService`), MIME parsing, Gmail OAuth, credential encryption |
| `auth` | Security config, registration/login, Google OIDC, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`, `PlanLimitService`), trial nudges |
| `team` | Team invites/membership |
| `digest`, `activation`, `reengagement`, `admin` | Scheduled emails and admin metrics (`/admin/*`) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Plan{FREE,PRO,BUSINESS}, Team*, SavedSearch, AuthEvent, …) |
| `repository` | Spring Data repositories |

## Data model overview
- `users` (V2) — email/password hash or Google subject (V18/V19), plan via `subscriptions` (V4, V17).
- `mail_accounts` (V5–V7, V29, V31, V35) — IMAP password or Gmail OAuth, polling policy, last-seen UID cursors, signature. Credentials encrypted (`CredentialEncryptor`).
- `email_threads` (V3, V9, V32) — owner-scoped, unread flag, **`conversation_key`** (participant-set key; the unit the chat UI is keyed on). `messages`, `participants`, `attachments`, `message_recipients` in V1; `messages.outbound` (V30). `attachments.blob_ref` is always null today (no downloadable bytes).
- `saved_searches` (V10–V11), `digest_email_preferences` (V12), `password_reset_tokens` (V14), `email_verifications` (V15), `auth_events` (V16), `teams`/`team_invites` (V24), `plan_change_events` (V26), `user_mail_preferences` (V34), `persistent_logins` (remember-me).
- Flyway migrations are immutable once merged.

## Auth model
- **Session-cookie, form login** (`POST /login`, params `email`, `password`, optional `remember-me`; 30-day persistent-token remember-me). Google OIDC login only when `GOOGLE_CLIENT_ID` is set.
- **CSRF is ON** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not in the permit-list in `SecurityConfig` requires an authenticated session.
- **There is no token/bearer auth.** A mobile client cannot authenticate against the server today (see `INTERNAL_TODO.md`).

## Web ↔ REST relationship
There is **no REST API**. All user-facing behaviour is server-rendered HTML via
Thymeleaf controllers. The only non-HTML responses are `POST /chats/refresh`
(JSON `{"imported":n}`), `GET /chats/events` (SSE), `/billing/webhook`, and PWA/SEO
assets. Controllers delegate to services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, `PlanLimitService`, …), so a future
`/api/v1` controller layer can reuse the same services; see `API.md` §Planned.

## Scheduled jobs
Mailbox polling, IMAP IDLE (paid tiers, flag `mailbox.idle.enabled`), weekly digest,
activation/re-engagement emails, trial-end conversion, admin weekly digest.
