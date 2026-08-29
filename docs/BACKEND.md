# Backend architecture

Spring Boot 3.5 / Java 21 monolith. It serves the ConexusMail SaaS: the
Thymeleaf website (marketing pages + authenticated web app), background
IMAP polling / IDLE, Stripe billing, and — going forward — a JSON REST API
that the mobile client in the sibling repo (`conexus-mail-mobile`)
consumes. Persistence is Spring Data JPA on top of PostgreSQL (H2 for
tests). Schema evolves via Flyway migrations (`V1__…` → `V35__…` at time
of writing).

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/       # onboarding nudge scheduler & service
├── admin/            # admin metrics (revenue, churn, funnels, digests)
├── auth/             # form + Google OIDC login, account, throttling
├── billing/          # Stripe checkout, portal, webhooks, plan limits
├── digest/           # weekly summary emails
├── domain/           # JPA entities (User, EmailThread, Message, …)
├── email/            # IMAP polling & IDLE, Gmail OAuth, MIME parsing
├── reengagement/     # re-engagement email scheduler
├── repository/       # Spring Data repositories
├── service/          # conversation model + IM transform (pure logic)
├── team/             # teams, invitations
└── web/              # Thymeleaf controllers, view models, chat SSE stream
```

Conventions: constructor injection only, package-private by default,
records for DTOs / view models, entities are classes.

## Persistence

- Base `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`.
- `application-local.yml` (`local` profile) points at
  `localhost:5432/email_messenger`; bootstrap that DB with `db/init.sql`.
- Tests use H2 (`src/test/resources/application.yml`).
- Migrations under `src/main/resources/db/migration/`; **immutable** once
  merged — add a new `V{n}__*.sql`, never edit an existing one.

Core tables (Flyway V1–V35): `users`, `mail_accounts`, `email_threads`,
`messages`, `message_recipients`, `attachments`, `participants`,
`subscriptions`, `plan_change_events`, `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`,
`email_verifications`, `auth_events`, `teams`, `team_members`,
`team_invites`, `user_mail_preferences`. The full history is the
migrations themselves.

## Authoritative state & business rules

The backend is the single source of truth for every persisted object.
The mobile client mirrors rules (see the sibling repo's
`docs/MOBILE.md`) but never authors state on its own — every mutation
round-trips through a server endpoint and is validated + recomputed
server-side. Plan limits (`billing/PlanLimitService`), conversation
grouping (`service/ConversationKeyService`), quoted-reply stripping
(`service/EmailBodyCleaner`, `service/IMTransformService`), sent-folder
reconciliation for outbound state (`email/MailboxPollingService`,
migration V31) and Sent-uid tracking all live here.

## Web + API share the service layer

The Thymeleaf controllers under `web/`, `auth/`, `billing/`, `team/`,
`admin/`, and `digest/` are thin — they delegate to services in the same
packages (or in `service/`). Any REST API added for the mobile client
must reuse those exact services; do NOT fork logic into JSON controllers.
See `docs/WEB.md` for the page/route inventory and `docs/API.md` for the
JSON contract.

## Live delivery

- `MailboxPollingScheduler` polls each connected mailbox on the account's
  configured cadence.
- `ImapIdleService` (paid tiers, flag `mailbox.idle.enabled`) keeps IDLE
  connections open and publishes `NewMailEvent` when the server signals
  new mail.
- Web clients subscribe to `GET /chats/events` (SSE, `ChatEventStream`);
  the mobile client will consume the same events over the API layer once
  a JSON push channel is defined (see `docs/API.md` → Streaming).

## Build & run

- Build: `./mvnw clean package`
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=ClassName#method`
- Local: `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
