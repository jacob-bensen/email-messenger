# Backend architecture

Spring Boot 3.5 / Java 21 / Maven monolith. It serves the public website and the
signed-in web app via Thymeleaf. **There is no REST API for a mobile client yet** — see
`docs/API.md` and `INTERNAL_TODO.md`.

> Deviation from the generic routine assumptions: persistence is **Spring Data JPA /
> Hibernate**, not jOOQ. There is no codegen step. Schema is owned by Flyway.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook gateways, plan limits (`PlanLimits`, `PlanLimitService`), trial-conversion emails, billing banners |
| `email` | IMAP (`JakartaImapClient`), IMAP IDLE, Gmail OAuth, `MimeMessageParser`, `EmailImportService`, `MailboxPollingService`, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService` (SMTP), view-model records (`ChatConversation`, `BubbleMessage`, ...) |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/legal/PWA/SEO), `ChatService`, `ConversationListService`, `ChatEventStream` (SSE) |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team` | Teams + invites |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails |
| `admin` | Admin revenue/retention metrics (`/admin/**`) |

## Data model (Flyway V1–V35, `src/main/resources/db/migration`)

Tables: `users`, `persistent_logins` (remember-me), `subscriptions`, `plan_change_events`,
`mail_accounts` (auth_type IMAP|OAuth, polling policy, last-seen UID cursors, signature),
`participants`, `email_threads` (owner, unread, `conversation_key`), `messages` (outbound flag),
`message_recipients`, `attachments` (`blob_ref` always null today), `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`,
`teams`, `team_members`, `team_invites`. (`thread_notes` created V25, dropped V33.)

Core concept: a **conversation** is keyed by `conversationKey` (hash of the participant set) and
merges every email thread with the same participants into one chat timeline.

## Auth model

- Session cookie (`JSESSIONID`) after form login (`POST /login`, params `email`, `password`) or
  Google OIDC; optional 30-day persistent remember-me cookie.
- CSRF is **enabled** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the `permitAll` list in `SecurityConfig` requires authentication.
- Controllers resolve the user with `UserService.requireByEmail(principal.getName())`; all
  queries are scoped to that owner.
- Plan tiers FREE/PRO/BUSINESS gate mailbox count / message history via `PlanLimitService`.
- No token (bearer/JWT) auth exists. A mobile client needs one — see `INTERNAL_TODO.md`.

## Web vs. API

All endpoints today are HTML (Thymeleaf) or redirect-after-POST. Only `POST /chats/refresh`
(JSON `{imported:n}`), `GET /chats/events` (SSE) and `POST /billing/webhook` return non-HTML.
Several flows contain business logic **inside the controller** (notably
`ConversationController.send`: recipient selection, signature appending, reply vs. new email,
outbound recording). That logic must be extracted into a service before a REST API can reuse it.

## Background work

`MailboxPollingScheduler` (poll cadence per `PollingPolicy`), `ImapIdleService` (paid tiers, flag
`mailbox.idle.enabled`), digest/re-engagement/activation/trial schedulers. New mail publishes
`NewMailEvent` → `ChatEventStream` → SSE `/chats/events`.
