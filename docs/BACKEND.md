# BACKEND

Architecture overview of `email-messenger` ("ConexusMail"). Reverse-engineered from
the code on 2026-10-02; anything not verified is marked TBD.

> Reality check vs. the generic two-repo template: this backend uses **Spring Data
> JPA / Hibernate** (not jOOQ), Flyway on PostgreSQL 16 (H2 in tests), Maven, and is
> a **server-rendered Thymeleaf app**. There is **no mobile REST API yet** — see
> `API.md`.

## Package layout (`com.emailmessenger`)

| Package | Responsibility |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing/landing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation building: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, ...) |
| `email` | Mailbox integration: `MimeMessageParser`, `EmailImportService`, IMAP polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, credential encryption |
| `auth` | Registration/login (form + Google OIDC), email verification, password reset, account settings, throttling, auth audit |
| `billing` | Stripe checkout/portal/webhook, plans, plan limits (`PlanLimitService`), banners/nudges |
| `team` | Teams and invites |
| `digest`, `reengagement`, `activation` | Scheduled emails |
| `admin` | `/admin/revenue` operator dashboard and metrics services |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Data model

35 Flyway migrations (`V1__init.sql` … `V35__mail_account_signature.sql`) in
`src/main/resources/db/migration`. Core entities: `User`, `MailAccount`
(IMAP password or Gmail OAuth), `EmailThread` (carries `conversationKey`, the
participant-set key the chats UI groups by), `Message`, `MessageRecipient`,
`Participant`, `Attachment` (`blob_ref` is always null — bytes are not stored),
`Subscription`, `PlanChangeEvent`, `SavedSearch`, `Team*`, token/audit tables.
Migrations are immutable; add a new `V{n}`.

## Auth model

Spring Security session auth (cookie `JSESSIONID`) with CSRF enabled
(ignored only for `/h2-console/**` and `/billing/webhook`). Form login at `/login`
and optional Google OIDC (only when client id/secret configured). Everything not on
the `permitAll` list in `SecurityConfig` requires authentication. **No token/API-key
auth exists**, so a mobile client has no supported way to authenticate today.

## Background work

- `MailboxPollingScheduler` → `MailboxPollingService` imports mail per account
  (polling policy by plan); sent-folder sync uses the V31 cursor.
- `ImapIdleService` (flag `mailbox.idle.enabled`) for paid tiers; publishes
  `NewMailEvent` → `ChatEventStream` → SSE `/chats/events`.
- Email schedulers: weekly digest, re-engagement, activation nudges,
  trial-end conversion, admin weekly digest.

## Web ↔ API relationship

Currently web only. Controllers delegate to services (`ChatService`,
`MailAccountService`, `OutboundMessageService`, `BillingService`, ...). A future
`/api/v1` layer must call the same services, not controllers.

## Build / test

See `CLAUDE.md`. Toolchain: Java 21, `./mvnw`.
