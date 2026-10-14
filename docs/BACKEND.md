# BACKEND architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail", repo `email-messenger`). It serves the
website (Thymeleaf) and owns all persistence and business rules.

> Classification note: the task template assumes jOOQ. The code actually uses
> **Spring Data JPA (Hibernate) + Flyway**; no jOOQ, no codegen step. Mismatch recorded in
> `INTERNAL_TODO.md`.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `SavedSearchController`, pricing/legal/PWA/SEO), SSE (`ChatEventsController`, `ChatEventStream`), `ChatService`, `ConversationListService`, `OutboundMessageService` |
| `service` | IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`; view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, ...) |
| `email` | Mailbox integration: Jakarta Mail IMAP client, IDLE (`ImapIdleService`), polling (`MailboxPollingService/Scheduler`), `EmailImportService`, `MimeMessageParser`, Gmail OAuth, `CredentialEncryptor` |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security config, form login + Google OIDC, registration, email verify, password reset, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits, billing banners |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digests, lifecycle emails, operator revenue/retention metrics |

## Schema
Flyway, `src/main/resources/db/migration/V1..V35`. Tables: users, mail_accounts, email_threads,
messages, message_recipients, participants, attachments, subscriptions, plan_change_events,
teams, team_members, team_invites, saved_searches, digest_email_preferences, thread_notes,
auth_events, email_verifications, password_reset_tokens, persistent_logins.
Conversations are keyed by a participant-set `conversationKey` (not by thread).
`attachments.blob_ref` is always null today (bytes not stored).

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 days) via form login (`POST /login`, params
`email`, `password`) or Google OIDC. **CSRF is on** (exempt: `/h2-console/**`,
`/billing/webhook`). Everything not on the permit-list in `auth/SecurityConfig.java` requires
authentication. There is **no token/bearer auth** yet.

## Web vs API
All current endpoints are server-rendered HTML (plus one SSE stream and PWA assets). There is
no JSON REST API. See `docs/API.md` for the proposed mobile contract (marked PROPOSED) and
`docs/WEB.md` for the page inventory. New `/api/v1/**` controllers must call the same
services the `web` controllers call (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`).

## Commands
`./mvnw clean package`, `./mvnw test`, `./mvnw test -Dtest=Class#method` (see CLAUDE.md).
