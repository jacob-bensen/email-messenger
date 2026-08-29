# BACKEND

## Overview

`email-messenger` is a Spring Boot 3 web application that imports email
threads over IMAP and renders them as a modern instant-messaging chat view
(product name: ConexusMail / MailIM). It is also the future authoritative
back-end for the `conexus-mail-mobile` Flutter client — see
`docs/API.md` for the (currently empty) REST contract that will grow to
serve the mobile app.

Today the backend serves TWO surfaces:

1. The public website + authenticated web app (server-rendered Thymeleaf).
   See `docs/WEB.md`.
2. Operator tooling (`/admin/*` under the admin authorizer).

A third surface — the mobile REST API — is planned but not yet
implemented. When it lands, every mobile endpoint MUST go through the
same service layer as the corresponding Thymeleaf page; do not fork
business logic between the two.

## Tech stack

- Java 21, Spring Boot 3.x (`web`, `thymeleaf`, `data-jpa`,
  `validation`, `mail`, `security`, `oauth2-client`, `oauth2-resource-server`).
- PostgreSQL 16 in dev/prod, H2 for tests. `application.yml` reads
  `JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`.
- Flyway migrations under `src/main/resources/db/migration/V{n}__*.sql`
  (currently through V35, immutable once merged).
- Maven with wrapper. Build: `./mvnw clean package`. Test:
  `./mvnw test`. Run locally: `JDBC_DATABASE_PASSWORD=… ./mvnw
  spring-boot:run -Dspring-boot.run.profiles=local`.
- Stripe SDK for billing (Checkout + Portal + webhooks).
- Jakarta Mail for IMAP + SMTP.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/          drip email nudges for new/lapsed users
├── admin/               operator metrics + revenue dashboard + digest
├── auth/                form + Google OIDC login, registration,
│                        password reset, email verification, throttling
├── billing/             Stripe checkout/portal/webhooks + plan limits
├── digest/              user-facing weekly digest opt-out
├── domain/              JPA entities
├── email/               IMAP polling + IDLE, Gmail OAuth, encryption,
│                        MIME parsing
├── reengagement/        winback flows for cold users
├── repository/          Spring Data JPA repositories
├── service/             conversation view model, IM transform, reply
│                        service, body cleaning
├── team/                team invites + membership
└── web/                 Thymeleaf controllers + view models + SSE
                         event stream (/chats/events)
src/main/resources/
├── templates/           Thymeleaf HTML
├── static/              CSS, JS, PWA assets
├── db/migration/        Flyway V1..V35
├── application.yml      base config
└── application-*.yml    per-profile overrides
```

## Domain model

Entities (all in `com.emailmessenger.domain`):

- `User` — authenticated principal.
- `Subscription` — one-per-user Stripe billing row. Enum `Plan`
  (`FREE`, `PERSONAL`, `TEAM`, `ENTERPRISE`), enum `BillingPeriod`,
  `status` (mirrors Stripe), `CancellationReason` when captured,
  `lastWinBackEmailSentAt` stamp.
- `MailAccount` — one connected mailbox (IMAP-password or Gmail OAuth),
  encrypted credentials, per-account signature, polling policy, sent-
  folder cursor (`last_seen_sent_uid`).
- `EmailThread` — grouped messages, includes `conversation_key`
  (participant-set hash) and unread flag.
- `Message`, `MessageRecipient`, `Attachment`, `Participant` — RFC 822
  structure.
- `Team`, `TeamMember`, `TeamInvite`, `TeamMemberRole` — shared
  workspace.
- `SavedSearch`, `DigestEmailPreference`, `PlanChangeEvent`,
  `AuthEvent`, tokens for email verification / password reset.

Service layer:

- `service.ConversationService` + view models
  (`Conversation`, `BubbleRun`, `SenderConversation`) build the
  IM-style presentation.
- `email.EmailImportService` + `MailboxPollingService` +
  `ImapIdleService` handle inbound mail (poll + IDLE).
- `web.OutboundMessageService` + `service.ReplyService` handle outbound.
- `billing.BillingService` mediates Stripe.
- `admin.*MetricsService` compute the operator dashboards.

## Auth model

Form login + Google OIDC (`auth/OAuthStartController`,
`GoogleOidcUserService`, `OAuth2ProvisioningService`). Sessions are
Spring Security's default `HttpSession`. Password reset and email
verification tokens are stored in DB and expire.

Login throttling: `LoginThrottleFilter` + `LoginThrottleService`.
Admin authorization: `admin.AdminAuthorizer` (env var `ADMIN_EMAILS`).

The mobile client will need a token-based auth path (bearer/refresh);
that endpoint does not exist yet. See `INTERNAL_TODO.md`.

## Live delivery

Paid tiers use IMAP IDLE (`email.ImapIdleService`, feature flag
`mailbox.idle.enabled`). New mail is published as a `NewMailEvent`
which the `web.ChatEventStream` forwards over SSE to `/chats/events`.
Web Push through the PWA service worker is a planned follow-up (see
`CLAUDE.md` roadmap).

## Where authoritative docs live

- `CLAUDE.md` — repo conventions + roadmap the routine works off.
- `master/PLAN.md` — the current Primary Objective (EPIC-18 win-back).
- `master/BACKLOG.md` — next 10 items advancing the PLAN.
- `master/CHANGELOG.md` — per-ship notes (very detailed).
- `master/MASTER_ACTIONS.md` — human-owner items (credentials, etc.).
- `docs/API.md` — REST contract for the mobile client (canonical).
- `docs/WEB.md` — Thymeleaf page inventory.
- `CHANGELOG.md` (root) — dated summary per full-stack sync run.
- `INTERNAL_TODO.md` — running list of gaps for the sync routine.

## Related repositories

- `jacob-bensen/conexus-mail-mobile` — Flutter mobile client.
  Consumes the API defined in `docs/API.md` and must mirror the
  server's business rules in its `lib/domain/` package.
