# BACKEND — architecture overview

Repo role: **BACKEND** (classified by evidence: `pom.xml`, Spring Boot annotations, Flyway migrations in
`src/main/resources/db/migration`). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently an empty scaffold).

## Stack (actual, verified in code)
- Java 21, Spring Boot 3, Maven wrapper, Thymeleaf, **Spring Data JPA / Hibernate** (`ddl-auto: validate`).
  NOTE: the generic routine template assumes jOOQ; this project does **not** use jOOQ — there is no codegen step.
- PostgreSQL 16 (prod/local), H2 (tests). Flyway migrations `V1`..`V35` (immutable once merged).
- Spring Security: form login + Google OIDC, remember-me (persistent tokens), CSRF **on**.
- Stripe billing (Free / Pro / Business), Jakarta Mail for IMAP/SMTP, Gmail OAuth for mailbox connect.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, saved searches, pricing/landing, PWA, SEO, legal), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation view models (`ChatConversation`, `ConversationListItem`, `BubbleRun`…), `ConversationKeyService`, `ReplyService` (SMTP send), `IMTransformService`, `EmailBodyCleaner` |
| `email` | IMAP polling/IDLE, MIME parsing, import (`EmailImportService`), mailbox credentials (`CredentialEncryptor`), Gmail OAuth |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Security config, registration, login throttle, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitKind`: THREAD_COUNT, MAILBOX_COUNT, SAVED_SEARCH_COUNT) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics (`/admin/revenue`) |

## Schema overview (tables)
`users`, `subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads` (has `conversation_key`, V32),
`messages`, `message_recipients`, `participants`, `attachments` (`blob_ref` always null today), `saved_searches`,
`teams`, `team_members`, `team_invites`, `auth_events`, `email_verifications`, `password_reset_tokens`,
`persistent_logins`, `digest_email_preferences`. (`thread_notes` dropped in V33.) Latest migration: `V35__mail_account_signature.sql`.

Core idea: a **conversation** = all messages with the same participant set, identified by `conversationKey`
(string ≤64 chars, derived by `ConversationKeyService` from recipients minus the owner's addresses).

## Auth model
- Browser session cookie (`JSESSIONID`) + remember-me; all routes except an allow-list in `SecurityConfig` require auth.
- CSRF enforced everywhere except `/h2-console/**` and `/billing/webhook` (Stripe signature instead).
- **There is no token/Bearer auth and no `/api/**` surface.** A mobile client cannot authenticate today (see `INTERNAL_TODO.md` #1).

## Web ↔ API relationship
All current endpoints are server-rendered Thymeleaf routes (see `WEB.md`). Only three return non-HTML:
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE), `POST /billing/webhook`. Business logic lives in services
(`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`) which a future REST layer must reuse.
Caveat: `ConversationController` currently contains orchestration (recipient parsing, sender-account choice,
signature appending, `MailAccount` selection) that is not in a service — extract before a REST controller needs it.
