# BACKEND

Server for the ConexusMail product. Serves the marketing/admin website
(Thymeleaf) AND — once the `/api/v1/**` layer described in
[`API.md`](API.md) is fleshed out — the REST API for the mobile client
in the sibling repo `conexus-mail-mobile`.

## Stack

- Java 21, Spring Boot 3
- Spring MVC (Thymeleaf), Spring Data JPA, Spring Security, Spring Mail
- PostgreSQL 16 (prod + local); H2 for tests
- Flyway for schema migrations (`src/main/resources/db/migration`)
- Jakarta Mail for IMAP/SMTP; GreenMail in tests
- Maven with wrapper (`./mvnw`)

The project's roadmap still names jOOQ in the target stack, but the
current code uses Spring Data JPA end-to-end. Reconcile in a later run.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java     Boot entry point
├── auth/       Users, sessions, form + Google OIDC, password reset,
│               email verification, account settings, throttling
├── billing/    Stripe checkout, portal, webhook, subscription lifecycle
├── domain/     JPA @Entity classes + enums (see "Domain model")
├── repository/ Spring Data repositories
├── email/      IMAP polling + IDLE, SMTP send, RFC 822 parsing,
│               body cleaner, threading, sent-folder sync
├── service/    ConversationKeyService, IM-transform utilities
├── team/       Teams, invites, membership
├── digest/     Weekly digest, opt-out
├── activation/ First-week nudges (day 3 / day 7 / last chance)
├── reengagement/ Long-idle user re-engagement
├── admin/      Operator dashboard: metrics, at-risk retention,
│               win-back outreach, weekly operator digest
└── web/        Thymeleaf controllers, form objects, view models,
                SSE ChatEventStream/ChatEventsController, PWA controller,
                landing/pricing/legal/SEO controllers
```

Templates live under `src/main/resources/templates/` (see
[`WEB.md`](WEB.md)) and static assets under
`src/main/resources/static/`.

## Authoritative service layer

The web (Thymeleaf) and — going forward — the REST API MUST share the
same service classes. Never fork business logic between a controller
that renders a page and a controller that returns JSON. Notable services
you will be reusing from a `/api/v1/**` controller:

| Service | Owns |
|---|---|
| `auth/UserService` | User CRUD, password hash, "current user" lookup |
| `auth/AccountService` | Profile updates, password/email changes |
| `web/ConversationListService` | Chat rail: filtering, paging, unread counts |
| `web/ChatService` | Open-conversation view model, mark-read |
| `web/OutboundMessageService` | Reply / new-thread send path |
| `email/MailboxPollingService` | Force-poll and background poll of accounts |
| `web/MailboxController`-adjacent `MailAccountService` | Mailbox CRUD |
| `web/SavedSearchService` | Saved-search CRUD |
| `team/TeamService` | Team + invite lifecycle |
| `billing/BillingService` | Checkout, portal, cancellation |
| `admin/*` | Operator metrics (already server-only) |

## Domain model (JPA entities)

Full field lists live in [`API.md`](API.md#entities) — this is a map.

- `User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`,
  `Participant`, `Attachment`, `SavedSearch`, `Team`, `TeamMember`,
  `TeamInvite`, `Subscription`, `PlanChangeEvent`,
  `DigestEmailPreference`, `EmailVerificationToken`,
  `PasswordResetToken`, `AuthEvent`.
- Enums: `AuthType`, `AuthEventType`, `TeamMemberRole`, `Plan`,
  `CancellationReason`, `RecipientType`.

Conversation grouping is by `EmailThread.conversationKey`: a hash of the
sorted lowercased non-owner participant emails, recomputed by
`ConversationKeyService` whenever a message is added.

## Live delivery pipeline

1. `MailboxPollingService` (@Scheduled) polls each `MailAccount` on
   its `nextPollAt` cadence.
2. Paid accounts also open an IMAP IDLE connection via
   `ImapIdleService` when `mailbox.idle.enabled=true`.
3. New messages are persisted, the owning thread's
   `conversation_key` is recomputed, and a `NewMailEvent` is published.
4. `ChatEventStream` fans events to any open `SseEmitter` from
   `GET /chats/events`.
5. The Thymeleaf chat page morphs the rail + pane in place from the
   SSE signal. The mobile client will do the same over the same SSE
   stream once auth is figured out.

## Sent-folder sync

Backfills messages the user sent from another client so the chat pane
matches Gmail/Apple Mail. Cursor lives on
`mail_accounts.last_seen_sent_uid` (migration V31); see
`SentFolderSyncService`.

## Outbound send

`OutboundMessageService.send(...)` builds an RFC 822 message using
`ReplyForm` values, sends via `JavaMailSender`, persists the resulting
`Message` with `outbound=true`, and updates the thread. Delivery-state
work (Sending/Sent/Delivered/Not Delivered) is on the roadmap in
`CLAUDE.md`.

## Auth

`auth/SecurityConfig` wires:
- Form login on `POST /login` (`email` + `password`)
- Remember-me cookie (`persistentTokenRepository`), 30-day validity
- Google OIDC when `GOOGLE_CLIENT_ID` is set
- `LoginThrottleFilter` before the form-login filter, driven by
  `LoginThrottleService`
- CSRF on for HTML forms; ignored for `/h2-console/**` and
  `/billing/webhook`
- `permitAll` for the public pages, verification, password reset,
  Stripe webhook, PWA and static assets. Everything else authenticated.

Admin endpoints are gated inside their controllers by
`AdminAuthorizer.requireAdmin(principal)` (no `@PreAuthorize`).

## Configuration

Base `application.yml` reads
`JDBC_DATABASE_URL` / `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`.
`application-local.yml` points at `localhost:5432/email_messenger`.
Tests use H2 via `src/test/resources/application.yml`. Feature flags:
`mailbox.idle.enabled`, `mailbox.polling.enabled` (see individual
services for the exhaustive list).

## Commands

```
./mvnw clean package                                # build
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
./mvnw test
./mvnw test -Dtest=ClassName#method
```

## Related repositories

See [`../CLAUDE.md`](../CLAUDE.md) "Related repositories".

## Open gaps

Tracked in [`../INTERNAL_TODO.md`](../INTERNAL_TODO.md). The largest is
the absence of a real REST API for the mobile client — today only
`POST /chats/refresh` returns JSON.
