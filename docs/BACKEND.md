# Backend architecture (email-messenger)

This document is the entry point for the server side of ConexusMail. The
same Spring Boot application serves the marketing/website (Thymeleaf) and,
in the future, the REST API for the mobile client.

See also:
- `docs/API.md` — the canonical REST API contract shared with the mobile client
- `docs/WEB.md` — Thymeleaf page inventory
- Sibling repo: `conexus-mail-mobile` (Flutter mobile client)

## Runtime

- Java 21, Spring Boot 3.5.x, packaged as a single JAR.
- PostgreSQL 16 in prod/local dev; H2 for tests.
- Flyway owns the schema (see `src/main/resources/db/migration/V*.sql`).
- JPA (Hibernate) for persistence with `ddl-auto: validate`. Repository
  layer is Spring Data (`repository/*Repository.java`).
- Server-side sessions via Spring Security (form login + Google OIDC).

Config lives in `src/main/resources/application.yml`; the `local` profile
adds `application-local.yml` pointing at `localhost:5432/email_messenger`.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java  Boot entry point
├── activation/     Onboarding activation nudges (day-1/3/7 emails)
├── admin/          Admin console (revenue, retention)
├── auth/           Login/register/password reset/OIDC/account
├── billing/        Stripe checkout, portal, webhook, plan gating
├── digest/         Daily/weekly email digest opt-out and delivery
├── domain/         JPA entities (User, MailAccount, EmailThread, Message …)
├── email/          IMAP polling + IDLE, MIME parsing, credentials
├── reengagement/   Win-back/re-activation email tracking
├── repository/     Spring Data repositories
├── service/        IM transform, conversation service, reply service
├── team/           Team invites and membership
└── web/            Thymeleaf @Controllers (all HTML today)
```

## Data model (essentials)

Owner-scoped: every mail-related row hangs off a `User` (`owner_id`).

- `User` — auth identity, plan, activation state, notification prefs.
- `MailAccount` — one IMAP/Gmail-OAuth mailbox for a user. Stores
  encrypted credentials, last-seen UID cursors (inbox + sent), signature.
- `EmailThread` — grouping of `Message` rows keyed by RFC-822 threading
  (`Message-ID` / `In-Reply-To` / `References`) plus a
  `conversationKey` (participant-set hash) used by the IM view.
- `Message` — one imported email. Body (plain + html), sent-at, direction
  (inbound/outbound), and a `MessageRecipient` fan-out (TO/CC/BCC).
- `Participant` — deduped by normalized email address. Display name +
  avatar. Not tied to a user unless they own an account.
- `Attachment` — filename, mime, size, `blob_ref` (currently null; blob
  storage is future work).
- `Subscription` / `PlanChangeEvent` — Stripe subscription state.
- `Team` / `TeamMember` / `TeamInvite` — team sharing (paid tier).
- `SavedSearch` — user-defined chat filter.

Reference: `V1__init.sql`; incremental migrations `V2..V35`.

## Ingest pipeline

1. `MailboxPollingScheduler` (@Scheduled, feature-flag
   `mailbox.polling.enabled`) fans work to `MailboxPollingService` per
   account on the configured cadence.
2. `MailboxPollingService` opens the mailbox via `ImapClient`
   (Jakarta Mail) with credentials from `ImapCredentialsResolver`
   (password or Gmail OAuth via `GmailOAuthClient`).
3. New UIDs → `MimeMessageParser` builds `ParsedEmail` → `EmailImportService`
   persists a `Message` and links / creates the `EmailThread` (deduping on
   Message-ID, threading by In-Reply-To/References).
4. `ConversationKeyBackfill` maintains the participant-set key used to
   collapse threads into IM conversations.
5. IDLE (`ImapIdleService`, `mailbox.idle.enabled`) publishes
   `NewMailEvent` which `ChatEventStream` broadcasts to SSE clients.

## IM transform

`service/IMTransformService` + `EmailBodyCleaner` strip quoted replies,
trim signatures out of the bubble body (they're lifted into structured
signature fields on the mailbox rail), and collapse consecutive same-sender
messages into `BubbleRun`s. `ConversationService` assembles the
`ChatConversation` view model consumed by the web chat page.

## Auth model

- Form login + OAuth2 (Google) via Spring Security.
- Sessions stored server-side (cookie). CSRF enabled on non-GET.
- `SecurityConfig` wires up the OIDC login only when
  `auth.google.client-id` and `-secret` are present.
- `@AuthenticationPrincipal User` (custom `UserDetails`) resolves the
  logged-in user in controllers.

For the mobile client we need a token-based scheme (bearer JWT or opaque
session token) instead of cookie sessions. See INTERNAL_TODO §API.

## Billing

Stripe. `BillingService` creates Checkout / Portal sessions;
`BillingWebhookController` (the only `@RestController` today) accepts
signed events. Plans (`Plan.FREE/PRO/BUSINESS`) gate features via
`PlanLimit`.

## Testing

`./mvnw test` runs JUnit 5 + Spring Boot slice tests against H2. Integration
tests use Testcontainers (Postgres) + GreenMail (SMTP/IMAP).

## Website ↔ API separation (target)

**Rule:** any business logic reachable from the mobile REST API MUST live
in a `service/` class the Thymeleaf controller and the (future) REST
controller both call. Never fork logic between the two paths.
