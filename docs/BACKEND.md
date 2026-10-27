# Backend architecture

Product: ConexusMail (repo `email-messenger`). Spring Boot 3 / Java 21 monolith that
imports email over IMAP and renders it as IM-style conversations. It serves a
server-rendered website (Thymeleaf) and — NOT YET — a REST API for the mobile client
(see `API.md`, `INTERNAL_TODO.md`).

> Reverse-engineered 2026-10-27 from the code. Unknowns are marked TBD.
> Stack deviation from the generic routine: persistence is **Spring Data JPA/Hibernate**
> (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen step.

## Stack
Spring Boot (web, thymeleaf, data-jpa, validation, mail, security, oauth2-client,
actuator), PostgreSQL 16 (prod/local), H2 (tests), Flyway (`V1`..`V35`), Maven wrapper,
Stripe SDK, Jakarta Mail (IMAP/SMTP).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing/landing/legal, PWA, SEO), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`), SSE (`ChatEventStream`) |
| `service` | Conversation logic: `ConversationService`, `ConversationKeyService` (participant-set key), `IMTransformService`/`EmailBodyCleaner` (quote stripping, bubbles), `ReplyService`; records `ChatConversation`, `ConversationListItem`, `BubbleMessage`, `SenderBubbleRun`, `ChatMember` |
| `domain` | JPA entities: `User`, `Subscription`, `Plan` (FREE/PRO/BUSINESS), `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `SavedSearch`, `Team*`, token/audit entities |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE (`MailboxPollingService`, `ImapIdleService`), MIME parsing, Gmail OAuth, credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, lifecycle emails, operator revenue dashboard (`/admin/**`) |

## Auth model (current)
- Session cookie (`JSESSIONID`) via form login `/login` (`email`, `password`), optional
  remember-me (30 days, JDBC token store), optional Google OIDC.
- CSRF enabled for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the permit-list in `auth/SecurityConfig` requires authentication;
  unauthenticated requests are redirected to `/login` (HTML), not answered with 401.
- Login throttling by email (`LoginThrottleFilter`).
- **No token/bearer auth exists.** A mobile client cannot authenticate today.

## Data
Core tables (V1): `participants`, `email_threads`, `messages`, `message_recipients`,
`attachments`; later migrations add users, subscriptions, mail_accounts (encrypted
credentials, polling policy, last-seen UIDs), saved_searches, teams, auth events,
plan change events, `email_threads.conversation_key`, outbound flag on messages.
Attachments: `blob_ref` is always null (bytes are not stored; not downloadable).

## Conversation model
A *conversation* is keyed by the set of non-owner participants (`conversationKey`),
merged across email threads into one timeline of bubble runs. Web routes are keyed by
that string: `/chats/{key}`.

## Web ↔ API relationship
Today the website is the only client. Controllers call services directly. When the REST
API is added it MUST call the same services (`ConversationListService`, `ChatService`,
`ReplyService`/`OutboundMessageService`, `MailAccountService`, …). Known risk: some
orchestration lives in `ConversationController` itself (mailbox scoping, signature
append, reply-vs-new-thread decision in `send`) and must be extracted to a service
before the API reuses it — tracked in `INTERNAL_TODO.md`.

## Live updates
`MailboxPollingService` publishes `NewMailEvent`; `ChatEventStream` fans out to open
SSE connections at `/chats/events`.
