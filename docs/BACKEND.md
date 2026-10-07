# Backend architecture

> Reverse-engineered from the code on 2026-10-07. Anything not verified is marked **TBD**.
> Sibling repo: `conexus-mail-mobile` (Flutter client). Contract: [`API.md`](API.md).

## Stack (actual)

Java 21, Spring Boot 3, **Spring Data JPA / Hibernate** (NOT jOOQ — there is no jOOQ
dependency or codegen), PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`,
V1…V35, immutable once merged), Thymeleaf, Spring Security (form login + Google OIDC +
persistent remember-me), Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP), jsoup, GreenMail
and Testcontainers in tests. Maven wrapper (`./mvnw`).

## Package layout (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = `/chats` workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `LegalController`, `PwaController`, `SeoController`), SSE (`ChatEventsController`, `ChatEventStream`), list/chat view services (`ConversationListService`, `ChatService`, `OutboundMessageService`) |
| `service` | Transform + domain services: `IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService` (participant-set key), `ReplyService`; view records `ChatConversation`, `ConversationListItem`, `ChatMember`, `SenderBubbleRun`, `BubbleRun`, `BubbleMessage` |
| `email` | Mailbox integration: IMAP client, IDLE (`ImapIdleService`), polling (`MailboxPollingService/Scheduler`), `EmailImportService`, `MimeMessageParser`, Gmail OAuth, `CredentialEncryptor` |
| `auth` | `SecurityConfig`, registration/login/reset/verify flows, account settings, Google OIDC, login throttling, auth audit events |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService` + `PlanLimits` (Free/Pro/Business), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle email schedulers, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (tables, from Flyway)

`users`, `persistent_logins`, `auth_events`, `email_verifications`, `password_reset_tokens`,
`mail_accounts` (IMAP password or Gmail OAuth; encrypted credentials), `participants`,
`email_threads`, `messages`, `message_recipients`, `attachments` (`blob_ref` always null —
bytes are not stored), `thread_notes`, `saved_searches`, `digest_email_preferences`,
`subscriptions`, `plan_change_events`, `teams`, `team_members`, `team_invites`.
Conversations in the UI are keyed by `conversationKey` (hash of the participant set) over
`email_threads`; one conversation merges many threads.

## Auth model

Session cookie (`JSESSIONID`) + CSRF token on every state-changing request, or a 30-day
`remember-me` cookie. Login: `POST /login` (`email`, `password`). Google: `/auth/google/start`
(only wired when `GOOGLE_CLIENT_ID` is set). Everything not in the permit-list in
`SecurityConfig` requires authentication. CSRF is disabled only for `/h2-console/**` and
`/billing/webhook` (Stripe signature). **There is no token/bearer authentication — a mobile
client cannot authenticate today** (see `API.md` §Planned, `INTERNAL_TODO.md`).

## Web ↔ API relationship

Today the app has **no REST/JSON API**. The Thymeleaf controllers are the only consumers of
the services. The only JSON endpoint is `POST /chats/refresh`; the only streaming endpoint
is `GET /chats/events` (SSE). Mobile support must be added as `/api/v1/**` controllers
that call the same services as `ConversationController` (see `API.md`). Caveat: some
orchestration currently lives *inside* `ConversationController` (account scoping
`selectMailbox`/`scopeAddress`, send-as-reply vs new-email branching, signature appending,
attachment conversion). It must be extracted into a service before an API controller is
added, otherwise the logic forks. Tracked in `INTERNAL_TODO.md`.

## Feature flags / config

`mailbox.idle.enabled` (IMAP IDLE), polling flag, Stripe + Google env vars, admin allow-list
(`AdminProperties`). Full list: **TBD** (see `application.yml`, `DEPLOY.md`).
