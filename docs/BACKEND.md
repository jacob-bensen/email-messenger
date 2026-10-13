# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf website plus
background mail ingestion. Source: `src/main/java/com/emailmessenger`.

> Reality check (2026-10-13): the persistence layer is **Spring Data JPA + Flyway**, not jOOQ.
> There is **no REST API for the mobile client yet** — see `docs/API.md`.

## Packages
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, saved searches, pricing/landing, legal, PWA, SEO), SSE `ChatEventStream`, `GlobalExceptionHandler`, model advices |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | Mailbox integration: `ImapClient`/`JakartaImapClient`, `ImapIdleService`, `MailboxPollingService` (+scheduler), `EmailImportService`, `MimeMessageParser`, Gmail OAuth, `CredentialEncryptor` |
| `service` | Conversation building: `ConversationService`, `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`; view records (`BubbleRun`, `ChatConversation`, ...) |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digest emails, lifecycle emails, admin metrics |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)
`users`, `subscriptions` (+ plan change events), `mail_accounts` (IMAP password or Gmail OAuth,
encrypted credentials, last-seen UID cursors, signature), `email_thread` (owner, unread,
`conversation_key` = participant-set key), `message` (+ recipients, outbound flag),
`participant` (deduped by normalized address), `attachment` (metadata only; `blob_ref` always null),
`saved_search`, `digest_email_preference`, `teams`/`team_invite`/`team_member`, auth/password/verification tokens, `auth_events`.
Migrations are immutable; add `V36+`.

## Auth model
Session cookie (form login or Google OIDC), CSRF enabled (ignored only for `/h2-console/**`, `/billing/webhook`).
Public allow-list is in `SecurityConfig`; everything else `authenticated()`. Admin routes gated by `AdminAuthorizer`.
There is no token/bearer auth for non-browser clients.

## Web ↔ API relationship
Controllers are thin and delegate to services (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailboxPollingService`, ...). Any future mobile REST controller MUST call the same services.

## Live updates
`MailboxPollingService` (and `ImapIdleService` for paid plans) publish `NewMailEvent`; `ChatEventStream` fans out over SSE at `/chats/events`.
