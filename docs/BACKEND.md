# Backend architecture

Repo role: **BACKEND** (classified by evidence: `pom.xml`, Spring Boot, Flyway
migrations, Thymeleaf templates). Sibling: `conexus-mail-mobile` (FRONTEND,
currently an empty Flutter placeholder — only a README).

## Actual stack (differs from the routine's assumed stack)

- Java 21, Spring Boot 3, Maven wrapper (`./mvnw`).
- Persistence is **Spring Data JPA / Hibernate** (`ddl-auto: validate`), NOT jOOQ.
  There is no jOOQ codegen step.
- PostgreSQL in prod/local, H2 in tests. Flyway migrations in
  `src/main/resources/db/migration` (35 files at time of writing; immutable once merged).
- Thymeleaf server-rendered UI. **No REST/JSON API exists yet** (see `docs/API.md`).

## Package layout (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = `/chats*`, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `PwaController`, `SeoController`, `LegalController`, `DemoController`), SSE (`ChatEventsController`, `ChatEventStream`), view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, onboarding) |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ConversationService`, `ReplyService`, bubble/run view records |
| `email` | Mailbox integration: `MimeMessageParser`, `EmailImportService`, IMAP (`JakartaImapClient`, IDLE via `ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), `MailboxPollingService`/`Scheduler`, `CredentialEncryptor`, `MailAccountService` |
| `auth` | Spring Security config, form login + Google OIDC, registration, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `team` | Teams and invites |
| `digest`, `reengagement`, `activation` | Scheduled emails |
| `admin` | Revenue/retention metrics (`/admin/revenue`) |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Auth model

- Session cookie (`JSESSIONID`) form login at `/login` (param `email`, `password`),
  optional remember-me cookie (30 days, JDBC token repo), optional Google OIDC.
- CSRF is **enabled** for everything except `/h2-console/**` and `/billing/webhook`.
- Everything not on the permit-all list in `auth/SecurityConfig.java` requires auth.
- No token/bearer auth exists. A mobile client would need one (see INTERNAL_TODO).

## Web vs API

All user-visible behavior is currently reached through Thymeleaf controllers that
call services directly. Controllers resolve the user with
`userService.requireByEmail(principal.getName())` and delegate to services
(`ChatService`, `MailAccountService`, `ReplyService`, ...). Only two JSON/stream
responses exist: `POST /chats/refresh` and the `/chats/events` SSE stream.

## Mailbox pipeline

Connect mailbox (IMAP password or Gmail OAuth) → `MailboxPollingService` imports via
`EmailImportService`/`MimeMessageParser` (threads by Message-ID/In-Reply-To/References,
sent-folder sync) → conversations keyed by participant-set `conversationKey` →
`NewMailEvent` → SSE `new-mail` to open browser tabs. Paid tiers get IMAP IDLE.

## Schema overview

See migrations `V1__init.sql` … latest. Core tables: users, subscriptions, mail_account,
email_thread, message, participant, message_recipient, attachment (`blob_ref` always null
today), saved_search, team/team_member/team_invite, auth events, plan change events.
Details beyond this: TBD (derive from migrations when needed).
