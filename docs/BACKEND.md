# BACKEND.md — email-messenger ("ConexusMail") architecture

Reverse-engineered 2026-10-29 from the code. Items not verifiable from code are marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate (`ddl-auto: validate`)** — *not jOOQ*, PostgreSQL 16 (H2 in tests),
Flyway (V1..V35 in `src/main/resources/db/migration`), Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: `/chats` workspace (`ConversationController`), mailboxes, Gmail OAuth connect, pricing/landing/legal/SEO/PWA, SSE (`ChatEventsController`, `ChatEventStream`), saved searches |
| `service` | Email → IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models (`ChatConversation`, `BubbleRun`…), `ConversationKeyService` (participant-set key), `ReplyService` |
| `email` | Import/polling: `MimeMessageParser`, `EmailImportService`, `MailboxPollingService` (+scheduler), `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `domain` / `repository` | JPA entities and Spring Data repos (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team…) |
| `auth` | Form login, Google OIDC, registration, email verification, password reset, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digest, lifecycle emails, admin revenue/retention metrics |

## Auth model
- Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token repo). Form login at `POST /login` (`email`,`password`); optional Google OIDC when `GOOGLE_CLIENT_ID` set.
- **CSRF is ON** (only `/h2-console/**` and `/billing/webhook` exempt). Everything not on the permit-list in `SecurityConfig` requires authentication.
- **There is no token/Bearer auth and no JSON REST API yet** — see `API.md` and `INTERNAL_TODO.md`.

## Data flow
Mailbox (IMAP password or Gmail OAuth) → `MailboxPollingService`/IDLE → `MimeMessageParser` → `EmailImportService` (threads by Message-ID/In-Reply-To/References, sets `conversation_key`) → `ChatService`/`ConversationListService` build views → templates. New mail publishes `NewMailEvent` → `ChatEventStream` (SSE `new-mail`). Outbound: `OutboundMessageService`/`ReplyService` over SMTP, recorded as outbound `Message` (V30).

## Schema overview
Core: `users`, `subscriptions`, `mail_accounts` (encrypted creds, last-seen UID cursors, polling policy, signature), `email_threads` (owner, unread, `conversation_key`), `messages`, `message_recipients`, `participants`, `attachments` (`blob_ref` always null — bytes not stored), `saved_searches`, `teams`/`team_members`/`team_invites`, token tables (password reset, email verification), `auth_events`, `plan_change_events`, `digest_email_preferences`, remember-me tokens. Flyway migrations are immutable; add `V36+`.

## Web vs API
All business logic lives in services; controllers are thin. Today only the web path exists. A mobile API must be added as `@RestController`s under `/api/v1` calling the **same** services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`, `BillingService`).
