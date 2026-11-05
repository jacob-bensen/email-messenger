# BACKEND.md — architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot 3.5.x, Flyway migrations
`V1`–`V35`, `@Controller`s). Sibling repo: `conexus-mail-mobile` (Flutter client,
currently an empty scaffold).

> Stack note: the routine's generic target mentions jOOQ. This repo uses
> **Spring Data JPA** (entities in `domain/`, repositories in `repository/`).
> We follow the code, not the template; no jOOQ codegen exists.

## Modules (`com.emailmessenger.*`)

| Package | Role |
|---|---|
| `auth` | Spring Security config (form login + Google OIDC + remember-me), registration, email verification, password reset, account settings, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook, plans & limits (`PlanLimitService`), trial nudges, billing banners |
| `email` | Mailbox integration: IMAP polling (`MailboxPollingService`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, MIME parsing, import (`EmailImportService`), credential encryption |
| `service` | Chat/IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `web` | Thymeleaf controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, mailboxes, saved searches, SEO, PWA, legal, SSE `ChatEventStream`) |
| `team` | Teams and invites |
| `digest`, `reengagement`, `activation` | Scheduled emails (digests, win-back, activation nudges) |
| `admin` | Revenue/funnel/churn metrics (`/admin/revenue`) |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (Flyway, immutable once merged)

Core: `participants`, `email_threads`, `messages`, `message_recipients`,
`attachments` (`blob_ref` always null today — attachment bytes not stored).
Accounts: `users`, `persistent_logins`, `email_verifications`,
`password_reset_tokens`, `auth_events`. Mail: `mail_accounts`. Billing:
`subscriptions`, `plan_change_events`. Product: `saved_searches`, `thread_notes`,
`digest_email_preferences`, `teams`, `team_members`, `team_invites`.
Conversations are keyed by a participant-set `conversationKey` (not by thread).

## Auth model

Session-cookie auth (JSESSIONID) with CSRF enabled; form login at `/login`
(`email`/`password`), Google OIDC when `GOOGLE_CLIENT_ID` is set, remember-me
(30 days, JDBC token store). Public routes are whitelisted in
`SecurityConfig`; everything else requires authentication. **There is no
token/bearer auth and no JSON REST API yet** — see `API.md`.

## Web vs. API

Today the website is the only first-party client. Controllers delegate to
services (`UserService`, `ChatService`, `ConversationListService`,
`MailAccountService`, `BillingService`, …). Exceptions: `ConversationController`
calls `MailboxPollingService` and `ConversationKeyService` directly and
contains list-filter parsing (`parseFilter`) and recipient parsing
(`parseRecipients`) that a future API would need to share — move into a
service before adding a REST layer (tracked in `INTERNAL_TODO.md`).
