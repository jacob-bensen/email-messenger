# Backend architecture

Reverse-engineered 2026-10-27 from the code. Unknowns are marked TBD.

**Stack:** Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no jOOQ dependency or codegen exists), Flyway (35 migrations, `V1`–`V35`), PostgreSQL 16 (H2 in tests), Thymeleaf, Maven wrapper. Stripe for billing, Jakarta Mail for IMAP/SMTP, Google OAuth for sign-in and Gmail mailbox connect.

## Package layout (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing, legal, PWA, SEO), SSE stream `ChatEventStream`, view services (`ChatService`, `ConversationListService`, `OutboundMessageService`) |
| `service` | Conversation building/IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, bubble/run view records |
| `email` | Mailbox integration: `ImapClient`/`JakartaImapClient`, `ImapIdleService`, `MailboxPollingService`+scheduler, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor`, `MailAccountService` |
| `domain` / `repository` | JPA entities and Spring Data repositories |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/win-back emails |
| `team` | Team invites and membership |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle / digest emails |
| `admin` | Revenue/funnel/churn metrics (`/admin/*`) |

## Data model (tables)

`users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts` (IMAP password or Gmail OAuth, per-account last-seen UID and sent-folder UID cursor, polling policy, signature), `email_threads` (owner, unread, `conversation_key`), `messages` (outbound flag), `message_recipients`, `participants` (deduped by normalized address), `attachments` (`blob_ref` always null today), `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`. (`thread_notes` was dropped in V33.)

A "chat" is keyed by `conversationKey` (participant set), merging multiple `email_threads`.

## Auth model

- Session-based (`JSESSIONID` + persistent remember-me token table). Form login at `POST /login` (`email`, `password`), Google OIDC via `/auth/google/start`. CSRF is on for form posts.
- Everything not on the `permitAll` list in `SecurityConfig` requires authentication. **There is no token/bearer auth and no JSON API** — a mobile client cannot authenticate today other than by emulating the browser session. See `INTERNAL_TODO.md`.
- `/admin/**` is gated by `AdminAuthorizer`.

## Web vs REST

There is no REST API. All user-facing behavior is server-rendered HTML plus form posts. The only non-HTML endpoints are the SSE stream (`/chats/events`), the Stripe webhook, PWA/SEO files. A future REST layer must call the same services the Thymeleaf controllers use (`ChatService`, `ConversationService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, `BillingService`).

## Live delivery

`MailboxPollingService` and `ImapIdleService` (paid tiers, `mailbox.idle.enabled`) publish `NewMailEvent`; `ChatEventStream` fans out to per-user SSE emitters.
