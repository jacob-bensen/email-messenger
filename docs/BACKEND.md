# BACKEND — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail" / email-messenger). Server-rendered
Thymeleaf website + background mail workers. Reverse-engineered 2026-10-23.

> Deviations from the generic target stack: persistence is **Spring Data JPA/Hibernate**
> (`ddl-auto: validate`), **not jOOQ** — no codegen step. There is **no REST API for a
> mobile client yet** (see `API.md`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, mailboxes, saved searches, pricing, PWA, SEO, legal), chat services, SSE `ChatEventStream` |
| `service` | Email → IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`) |
| `email` | IMAP/SMTP import: `MailboxPollingService/Scheduler`, `ImapIdleService`, `MimeMessageParser`, `EmailImportService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security config, registration, Google OIDC, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digests, lifecycle emails, operator revenue dashboard |

## Schema (Flyway `src/main/resources/db/migration`, V1–V35, immutable)
Tables: users, participants, email_threads, messages, message_recipients, attachments
(`blob_ref` always null — bytes not stored), mail_accounts, subscriptions,
plan_change_events, saved_searches, teams, team_members, team_invites, auth_events,
email_verifications, password_reset_tokens, persistent_logins, digest_email_preferences
(thread_notes dropped in V33). Next migration: **V36**.

Core model: a *conversation* is keyed by `conversationKey` (hash of participant set),
merging all `EmailThread`s with the same participants into one chat timeline.
Plans: `FREE`, `PRO`, `BUSINESS` (`domain/Plan`), caps in `PlanLimitService`.

## Auth model
Spring Security, **session cookie (JSESSIONID) + remember-me (30d, JDBC token repo)**,
form login (`/login`, params `email`/`password`) and optional Google OIDC (only when
`GOOGLE_CLIENT_ID` set). **CSRF is on** (exempt: `/h2-console/**`, `/billing/webhook`).
No token/JWT/API-key auth exists. Principal name = user email.

## Web vs. API
All authenticated routes are HTML (Thymeleaf) except: `POST /chats/refresh` (JSON),
`GET /chats/events` (SSE), `POST /billing/webhook`. Controllers delegate to services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `ReplyService`,
`MailAccountService`, …); a future REST layer must reuse those same services.

## Config / run
See `CLAUDE.md`. Env: `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_CLIENT_*`, `STRIPE_*`,
credential-encryption secret. Tests use H2 (`src/test/resources/application.yml`).
