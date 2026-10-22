# BACKEND — architecture (ConexusMail server)

Repo classification: this repo is the **BACKEND** (pom.xml, Spring Boot, Flyway
migrations `src/main/resources/db/migration/V1..V35`). Its sibling is the Flutter
client `conexus-mail-mobile` (FRONTEND).

## Stack (actual, as of 2026-10-22)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (NOT jOOQ — there is no codegen
step), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Maven wrapper (`./mvnw`).
Jakarta Mail for IMAP/SMTP, Stripe SDK for billing, Spring Security (form login,
Google OIDC, persistent remember-me).

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, landing/pricing/legal/PWA/SEO), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`), SSE (`ChatEventsController`, `ChatEventStream`) |
| `service` | Domain services + view-model records: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; records `ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`, `ChatMember` |
| `email` | Mailbox integration: `MailboxPollingService`/`Scheduler`, `ImapIdleService`, `JakartaImapClient`, `MimeMessageParser`, `EmailImportService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `repository` | Spring Data repositories |
| `auth` | Security config, registration/login/reset/verify, account settings, Google OIDC |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digests, admin metrics, lifecycle emails |

## Data model (tables)
users, subscriptions, plan_change_events, mail_accounts (auth_type: IMAP password |
Gmail OAuth; last_seen_uid / last_seen_sent_uid cursors; signature), email_threads
(owner, `conversation_key`, unread), messages (outbound flag, reply-to), message_recipients,
participants (deduped by normalized email), attachments (`blob_ref` always NULL — bytes
not stored), saved_searches, teams, team_members, team_invites, digest_email_preferences,
auth_events, email_verifications, password_reset_tokens, persistent_logins.

A **conversation** = all threads sharing a `conversation_key` (participant-set key from
`ConversationKeyService`), merged into one chat timeline per owner (+ optional mailbox scope).

## Auth model
Session-cookie (JSESSIONID) form login at `POST /login` (`email`, `password`,
`remember-me`) with CSRF **enabled** (only `/h2-console/**`, `/billing/webhook` exempt),
plus Google OIDC. Everything not on the public allow-list in `auth/SecurityConfig` requires
an authenticated session. **There is no token/Bearer auth** — see `INTERNAL_TODO.md`.

## Web vs. API
Today the app has **no REST API**; the website is server-rendered and form-driven. The
only JSON endpoint is `POST /chats/refresh`. Controllers fetch the user via
`UserService.requireByEmail(principal.getName())` and delegate to services, so a future
`/api/v1` layer must reuse `ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, `BillingService` — not the controllers.
