# BACKEND.md — architecture

Reverse-engineered 2026-11-06 from the code. Unknowns are marked **TBD**.

## Classification
This repo is the **BACKEND** (Maven, Spring Boot 3, Flyway migrations, `@Controller`s). It also serves the
public website and the logged-in web app via Thymeleaf. Sibling: `conexus-mail-mobile` (Flutter client).

> Deviation from the generic routine assumptions: persistence is **Spring Data JPA/Hibernate** (`ddl-auto: validate`),
> **not jOOQ**. There is no codegen step. Flyway owns the schema.

## Stack
Java 21, Spring Boot 3.x (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client), Flyway,
PostgreSQL 16 (prod/local), H2 (tests), Jakarta Mail (IMAP/SMTP), Stripe.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…) |
| `email` | IMAP/SMTP integration: `EmailImportService`, `MimeMessageParser`, `MailboxPollingService/Scheduler`, `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor`, `MailAccountService` |
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, …) |
| `repository` | Spring Data repositories |
| `auth` | Security config, form login + Google OIDC, registration, password reset, email verification, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `Plan` FREE/PRO/BUSINESS) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics (`/admin/revenue`) |

## Data model (tables, see `db/migration/V1..V35`)
users, subscriptions, plan_change_events, mail_accounts, email_threads (has `conversation_key`, owner),
messages (has outbound flag), message_recipients, participants, attachments (`blob_ref` always null today),
saved_searches, teams/team_members/team_invites, digest_email_preferences, password_reset_tokens,
email_verifications, auth_events, persistent_logins. (`thread_notes` dropped in V33.)

## Auth model
Session-cookie auth (Spring Security form login at `/login`, field `email`/`password`; Google OIDC if
`GOOGLE_CLIENT_ID` set; remember-me 30 days, DB-backed). **CSRF is enabled** (exempt: `/h2-console/**`,
`/billing/webhook`). Everything not in the `permitAll` list is authenticated. There is **no token / bearer auth**,
so a mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
All controllers are server-rendered MVC. The only JSON endpoint is `POST /chats/refresh`; the only streaming
one is `GET /chats/events` (SSE). **No REST API for the mobile client exists yet** — see `docs/API.md`.
Business logic lives in service classes (`ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`); controllers resolve the user from `Principal` and delegate.
Any future REST controller must call these same services.

## Background jobs
`MailboxPollingScheduler` (flag `mailbox.polling.enabled`), `ImapIdleService` (`mailbox.idle.enabled`, paid
plans), weekly digest, activation/re-engagement/trial-end schedulers, admin weekly digest.

## Config
Env: `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_CLIENT_*`, `STRIPE_*`, `MAILBOX_ENCRYPTION_PASSWORD/SALT`,
`MAILBOX_POLLING_ENABLED`, `MAILBOX_IDLE_ENABLED`. Profiles: `local`, tests use H2.
