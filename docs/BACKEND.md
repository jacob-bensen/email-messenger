# BACKEND — architecture (as implemented)

Spring Boot 3 / Java 21 app, package `com.emailmessenger`. Persistence is
**Spring Data JPA** (not jOOQ) on PostgreSQL 16 (H2 in tests), schema owned by
Flyway (`src/main/resources/db/migration`, V1..V35, immutable once merged).

## Modules (packages)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing/landing, legal, SEO, PWA shell), SSE stream `/chats/events`, `ConversationListService`, `ChatService`, `OutboundMessageService` |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…) |
| `email` | IMAP/Gmail import: `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `MailAccountService`, `GmailOAuthClient` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plans (Free/Pro/Business), `PlanLimitService` |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Schema overview (tables)
users, subscriptions, plan_change_events, auth_events, persistent_logins,
email_verifications, password_reset_tokens, mail_accounts, email_threads
(has `conversation_key`, owner, unread), messages (has outbound flag),
message_recipients, participants, attachments (`blob_ref` always null today),
saved_searches, digest_email_preferences, teams, team_members, team_invites.
(`thread_notes` was dropped in V33.)

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30d, JDBC token repo).
Form login at `POST /login` (`email`, `password`), optional Google OIDC when
`GOOGLE_CLIENT_ID` is set. **CSRF is on** (exempt: `/h2-console/**`,
`/billing/webhook`). Everything not in the `permitAll` list in
`SecurityConfig` requires authentication. **There is no token/bearer auth and
no JSON API** — see `docs/API.md`.

## Web ↔ API relationship
Today the web UI is the only client. Controllers resolve the user via
`UserService.requireByEmail(principal)` and call services
(`ConversationListService`, `ChatService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`). The planned `/api/v1` layer
must reuse these services; do not add logic to controllers.
Known exception: `POST /chats/refresh` already returns JSON (`{"imported":n}`).
