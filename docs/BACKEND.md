# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). It serves the public website and
the logged-in web app via Thymeleaf, owns all persistence, and is the authority for
business rules. Sibling repo: `conexus-mail-mobile` (Flutter client).

> Reverse-engineered 2026-10-21 from the code. Unknowns are marked TBD.
> Deviations from the generic stack assumption: persistence is **Spring Data JPA /
> Hibernate** (`ddl-auto: validate`), **not jOOQ**; there is **no REST/JSON API yet**
> (see `API.md`).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, legal, pricing, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Chat view models + transform: `IMTransformService`, `EmailBodyCleaner` (quote/signature stripping), `ConversationKeyService` (participant-set key), `ReplyService` |
| `email` | IMAP/SMTP: `MailboxPollingService` (+scheduler), `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor` |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `auth` | Spring Security config, form login, Google OIDC, registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/win-back jobs |
| `team` | Team invites (Business plan) |
| `digest`, `activation`, `reengagement`, `admin` | Scheduled emails and `/admin/**` metrics (allowlist in `admin.emails`) |

## Data model (Flyway V1..V35, immutable once merged)

Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`,
`email_threads` (owner-scoped, `conversation_key`), `messages`
(`outbound` flag from V30), `message_recipients`, `participants`, `attachments`
(`blob_ref` always null today), `saved_searches`, `digest_email_preferences`,
`teams`, `team_members`, `team_invites`, `password_reset_tokens`,
`email_verifications`, `auth_events`, `persistent_logins`.

Core idea: a *chat* is the set of threads sharing a participant-set
`conversationKey` (V32); the IM view merges them into one timeline of bubble runs.

## Auth model

Session cookie (form login + remember-me, optional Google OIDC) with CSRF on every
form. Only `/billing/webhook` and `/h2-console/**` ignore CSRF. Everything not
explicitly permitted requires authentication. **No token auth exists** — mobile
needs one (see `API.md` §Planned, `INTERNAL_TODO.md`).

## Web vs API

All user-facing behaviour is in Thymeleaf controllers calling services
(`ChatService`, `ConversationListService`, `MailAccountService`,
`OutboundMessageService`, `PlanLimitService`). Some orchestration still lives in
`ConversationController` (mailbox selection, send flow, signature appending) and
must move into a service before a REST controller can reuse it.
