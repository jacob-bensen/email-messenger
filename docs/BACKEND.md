# Backend architecture

Spring Boot 3 monolith. Java 21, Maven, Spring Data JPA, PostgreSQL 16 in
production and local dev (H2 for tests), Flyway for migrations, Thymeleaf
for the server-rendered website.

Product name: **ConexusMail** (email-messenger is the repo slug).

## Roles this repo plays

The backend is the authoritative system for:

1. Persistence and business logic for all product data (users, mail
   accounts, threads, messages, subscriptions, teams, saved searches,
   digests).
2. The **public website** (server-rendered Thymeleaf, plus a PWA shell).
3. IMAP polling and IDLE for connected mailboxes; SMTP for outbound
   messages sent from the chat pane.
4. Stripe billing (checkout, portal, webhooks) and admin metrics.
5. Auth for humans in the browser (form login + Google OIDC, session
   cookies + remember-me).

The mobile client in `../conexus-mail-mobile` will consume a REST API
served by this same app. **That REST API does not exist yet** — see
`docs/API.md` and `INTERNAL_TODO.md`. When it is built, controllers,
services, and repositories are shared with the Thymeleaf paths — never
forked.

## Package layout (`com.emailmessenger`)

- `EmailMessengerApplication` — Spring Boot entry point.
- `web/` — Thymeleaf controllers, form objects, PWA shell, chat pane, SSE
  stream (`ChatEventStream`, `ChatEventsController`).
- `service/` — pure business logic: conversation assembly
  (`ConversationService`, `ConversationListService`), IM transform
  (`IMTransformService`, `EmailBodyCleaner`), reply composition
  (`ReplyService`), participant-set keys (`ConversationKeyService`).
- `domain/` — JPA entities (`EmailThread`, `Message`, `Participant`,
  `MailAccount`, `User`, `Subscription`, `Team`, `SavedSearch`, …) and
  the enums they use (`Plan`, `AuthType`, `RecipientType`,
  `CancellationReason`, `DigestEmailPreference`, `TeamMemberRole`, …).
- `repository/` — Spring Data JPA repositories.
- `email/` — mailbox integration: IMAP (`ImapClient`, `JakartaImapClient`,
  `ImapIdleService`), Gmail OAuth (`GmailOAuthClient`), MIME parsing
  (`MimeMessageParser`), scheduled polling
  (`MailboxPollingScheduler`, `MailboxPollingService`), credential
  encryption (`CredentialEncryptor`), sent-folder cursor backfill.
- `auth/` — Spring Security config (`SecurityConfig`), form login,
  password reset, email verification, Google OIDC user provisioning,
  login throttling, remember-me (JDBC-backed).
- `billing/` — Stripe checkout/portal/webhook gateways
  (`StripeCheckoutGatewayImpl`, `StripePortalGatewayImpl`,
  `StripeWebhookGatewayImpl`), plan limit enforcement
  (`PlanLimitService`, `PlanLimits`, `PlanLimitKind`), trial nudges.
- `team/` — team invitations and membership.
- `digest/` — weekly digest email opt-out endpoint and scheduler.
- `activation/`, `reengagement/` — lifecycle nudges (scheduled).
- `admin/` — operator-only revenue, churn, funnel and retention metrics;
  win-back outreach.

## Data & schema

Flyway migrations live under `src/main/resources/db/migration/`
(currently `V1__init.sql` through `V35__mail_account_signature.sql`).
Migrations are immutable once merged; always add a new `V{n}__*.sql`.

Local dev uses PostgreSQL 16. Bootstrap the local database with
`db/init.sql`. Tests use H2 through `src/test/resources/application.yml`.

Base `application.yml` reads `JDBC_DATABASE_URL` /
`JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` from the environment;
the `local` profile in `application-local.yml` points at
`localhost:5432/email_messenger`.

## Auth model today

Browser-only. Spring Security form login and Google OIDC issue a session
cookie (`JSESSIONID`) with optional `remember-me` (30-day JDBC token,
`persistent_logins` table). CSRF is enabled except on `/billing/webhook`
and `/h2-console`. Login attempts run through `LoginThrottleFilter`
before the credentials filter.

A **mobile-friendly auth path** (token endpoint, refresh, revoke) does
not exist yet. Adding it is the prerequisite for anything on the mobile
client that reads user data. See `docs/API.md` §Auth for the shape we
plan to expose.

## Realtime

`MailboxPollingService` publishes a `NewMailEvent` when the poller (or
IDLE) commits new messages. `ChatEventStream` fans that to per-user
Server-Sent Events subscribers. `ChatEventsController` exposes the
stream at `GET /chats/events`, and `fragments/live-refresh.html` uses
it to morph the rail and the open chat pane in place.

The mobile client will need an equivalent stream — either SSE reused
(cheap) or a WebSocket (nicer on mobile networks). Decision deferred;
tracked in `INTERNAL_TODO.md`.

## Business rules that belong to the server

Every plan-gated action goes through `PlanLimitService` /
`PlanLimits`. The mobile client MUST NOT re-derive limits from a client
copy of the plan config — it asks the server and shows what the server
returns. Stripe subscription state is authoritative for plan level;
`Subscription.status` and `Subscription.currentPeriodEnd` drive gating.

Conversation identity is a normalized participant-set hash
(`ConversationKeyService`). The mobile client uses whatever
`conversationKey` the server returns — it does not compute keys on its
own.

## Local commands

- Build: `./mvnw clean package`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`
- Run locally: `JDBC_DATABASE_PASSWORD=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

## Related documents

- `docs/API.md` — REST contract the mobile client depends on.
- `docs/WEB.md` — Thymeleaf website inventory.
- `../conexus-mail-mobile/docs/MOBILE.md` — mobile client architecture.
- `../conexus-mail-mobile/docs/DESIGN.md` — product/design source of truth.
