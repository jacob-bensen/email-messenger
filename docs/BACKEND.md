# BACKEND.md — email-messenger / ConexusMail server

> Architecture reference for the Spring Boot app that owns persistence,
> business logic, and the public/Thymeleaf website. See `docs/API.md` for
> the (still-emerging) mobile REST contract and `docs/WEB.md` for the
> Thymeleaf page inventory.

## Stack

- Java 21, Spring Boot 3.5.x (`spring-boot-starter-parent`).
- Persistence: **Spring Data JPA** on PostgreSQL 16 (Flyway migrations
  under `src/main/resources/db/migration/`, currently V1..V35). H2 for
  tests. The generic full-stack routine's "jOOQ" default does **not**
  apply — this project uses JPA. `docs/API.md` and any mobile-side
  documentation must reflect that.
- Web: `spring-boot-starter-web` + Thymeleaf for server-rendered pages;
  Spring Security (form login + remember-me + OAuth2 Google login).
- Mail: `spring-boot-starter-mail` + Jakarta Mail for IMAP/SMTP;
  GreenMail in tests. Live-delivery uses IMAP IDLE
  (`ImapIdleService`) fanned out through an SSE stream
  (`ChatEventStream`).
- Billing: Stripe (Checkout + Customer Portal + webhooks).
- Build: Maven (wrapper); JUnit 5 + Testcontainers integration tests.

## Package layout (`com.emailmessenger`)

| Package | Contents |
| --- | --- |
| `EmailMessengerApplication` | `@SpringBootApplication` entry point |
| `activation` | Onboarding/activation nudge job + service |
| `admin` | Admin metrics services + `/admin/revenue` controller |
| `auth` | User registration, form/Google OIDC login, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | `BillingService`, plan-limit enforcement, Stripe webhook |
| `digest` | Weekly digest job + opt-out page |
| `domain` | JPA entities + enums (see below) |
| `email` | Mailbox connect + IMAP polling + IDLE + import + owner-address heuristics |
| `reengagement` | Cold-user re-engagement emails |
| `repository` | Spring Data JPA repositories |
| `service` | Conversation/IM transform/reply/demo services |
| `team` | Teams + invites |
| `web` | Thymeleaf controllers, chat services, saved searches, SSE, PWA, SEO, legal |

## Domain model

Entities live in `com.emailmessenger.domain`; view models live alongside
their consumers. Full list:

Users & auth: `User`, `EmailVerificationToken`, `PasswordResetToken`,
`AuthEvent`, `AuthEventType`, `AuthType`.

Billing: `Subscription`, `Plan`, `PlanChangeEvent`, `CancellationReason`.

Mailboxes & email: `MailAccount`, `EmailThread`, `Message`,
`MessageRecipient`, `RecipientType`, `Participant`, `Attachment`.

Teams & prefs: `Team`, `TeamMember`, `TeamMemberRole`, `TeamInvite`,
`SavedSearch`, `DigestEmailPreference`.

## Conversation model (`conversationKey`)

The `/chats` workspace does not surface individual `EmailThread` rows —
it groups messages by the **participant-set** they belong to. The key is
computed by `ConversationKeyService` from the deduped set of
`Participant` addresses on a message (excluding the mailbox owner) and
persisted on `EmailThread.conversation_key` (Flyway V32). All chat-list
and chat-pane queries flow through `ConversationListService` and
`ChatService`.

## Authoritative rules (server-only)

All state mutations run through the service layer, in a transaction,
with the server recomputing anything security- or economy-relevant:

- Plan limits — mailbox count, polling frequency, feature access —
  come from `PlanLimitService` (never trust a client-supplied plan).
- Owner detection for outbound/inbound classification is done by
  `OwnerAddressService`, not by trusting message headers alone.
- Stripe events are verified with `Stripe-Signature` in
  `StripeWebhookGateway` before `BillingService` mutates a subscription.
- Threading, participant dedup, quote-stripping and IM bubble grouping
  happen server-side in `EmailImportService` + `IMTransformService`;
  the client renders what the server produced.

**When the mobile client lands, `lib/domain/` must mirror these rules
(pure Dart, no Flutter) but must never be treated as authoritative —
the server always re-validates on write.**

## Auth model

Today the site uses session-based authentication (Spring Security form
login + remember-me cookie + OAuth2 Google login). This is fine for the
Thymeleaf UI. It is **not** suitable as-is for a mobile client:

- No token endpoint exists yet.
- No CSRF-free / stateless API surface has been split out.
- `SecurityConfig` allow-lists the marketing/auth/PWA routes; every
  other path requires a session.

The mobile REST surface (see `docs/API.md`) will need its own
authenticator (token or per-device credential) layered onto the same
`UserService`.

## Website ↔ API service reuse rule

Thymeleaf controllers and any future REST controllers **must call the
same service layer**. Never fork validation, plan-limit, or import logic
into a controller or template. If a service method does not exist for a
piece of business logic, add it to `service/`, `email/`, `billing/`,
`auth/` or `web/` as appropriate and let both the web page and the API
endpoint call through.

## Live-delivery pipeline

1. `MailboxPollingService` (`@Scheduled`) polls each `MailAccount`.
2. For paid accounts with IDLE-capable IMAP, `ImapIdleService` holds an
   IDLE connection and publishes `NewMailEvent`s.
3. `ChatEventStream` fans out per-user SSE frames on `/chats/events`.
4. The chat page's fragment refresh (`fragments/live-refresh.html`)
   morphs the chat list + open pane in place.

Web Push (part 2 in the roadmap) is not yet implemented.

## Testing

- `./mvnw test` for unit + slice + integration tests.
- Testcontainers provides Postgres; GreenMail provides SMTP/IMAP.
- Test packages mirror main packages.

## Runtime configuration

- `application.yml` reads `JDBC_DATABASE_URL`,
  `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD` from the env.
- `application-local.yml` (activated with
  `-Dspring-boot.run.profiles=local`) points at
  `jdbc:postgresql://localhost:5432/email_messenger` — bootstrap the
  db + role with `db/init.sql`.
- Google OAuth is configured via `GoogleOAuthProperties`; keys are
  read from env at startup.

## What the mobile client will need from the backend

Recorded here so the eventual API design starts from real requirements
(not a made-up feature list). See `docs/API.md` for the tracker.

- Auth: sign in / sign up / OAuth handoff for mobile; per-device
  token issuance + rotation.
- Mailboxes: list, add (IMAP + Gmail OAuth), sync, delete, edit prefs.
- Chats: list conversations (by `conversationKey`), read a pane
  (messages, participants, signatures), send a new message, reply.
- Live delivery: an SSE or WebSocket equivalent of `/chats/events` for
  in-app updates.
- Push: Web Push exists as a roadmap item; mobile will want FCM/APNs
  registration on the same subscription table when it lands.
- Account: read plan/limits, open Stripe portal from mobile,
  reconnect Gmail OAuth.
