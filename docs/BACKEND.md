# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-26. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — the routine template
assumes jOOQ; this repo uses JPA entities + Spring Data repositories),
PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`,
V1–V35), Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers for the app (`/chats`, `/mailboxes`, `/searches`), marketing/legal/SEO/PWA, SSE (`ChatEventStream`), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, `ConversationService` |
| `email` | IMAP/Gmail import: `MailboxPollingService`, `ImapIdleService`, `MimeMessageParser`, `EmailImportService`, `CredentialEncryptor`, `GmailOAuthClient` |
| `auth` | `SecurityConfig`, form login + Google OIDC, registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService`, trial conversion |
| `team` | Team invites and membership |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |
| `admin` | `/admin/revenue` metrics, win-back outreach |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema overview (tables)
`users`, `auth_events`, `persistent_logins` (remember-me), `password_reset_tokens`,
`email_verifications`, `mail_accounts` (encrypted IMAP creds / Gmail OAuth
tokens, per-account last-seen UID cursors), `email_threads`, `messages`,
`message_recipients`, `participants`, `attachments` (`blob_ref` always null —
bytes are not stored), `saved_searches`, `subscriptions`, `plan_change_events`,
`teams`, `team_members`, `team_invites`, `digest_email_preferences`.
(`thread_notes` was dropped in V33.) Conversations are keyed by a
participant-set `conversationKey`, computed by `ConversationKeyService`.

## Auth model
Session-cookie auth (form login at `/login`, param `email`/`password`;
optional Google OIDC when `GOOGLE_CLIENT_ID` is set), remember-me (30 days,
JDBC token repo), CSRF **enabled** (ignored only for `/h2-console/**` and
`/billing/webhook`). Everything not in the `permitAll` list in
`SecurityConfig` requires authentication. **There is no token/bearer auth.**

## Web vs REST
All HTTP surface today is server-rendered Thymeleaf plus a few JSON/SSE
helpers. There is **no REST API for a mobile client**. See `API.md` and
`INTERNAL_TODO.md`.
