# BACKEND

Spring Boot 3 / Java 21 monolith that serves both the ConexusMail website
(server-rendered Thymeleaf) and the JSON REST API consumed by the mobile
client. This document describes the module layout and where each concern
lives.

**Related repos:** `jacob-bensen/conexus-mail-mobile` (Flutter client).
The mobile client depends on `docs/API.md` (canonical contract). Any
change to that contract must land in the same commit set as changes to
the Spring controllers and the mobile Dart client.

## Runtime

- Java 21 (LTS), Spring Boot 3.x.
- PostgreSQL 16 in prod and local dev (`db/init.sql`); H2 for tests.
- Persistence: Spring Data JPA (not jOOQ — the project pre-dates the
  team's later default; see INTERNAL_TODO.md).
- Flyway migrations under `src/main/resources/db/migration`
  (`V{n}__{name}.sql`, immutable once merged).
- Auth: form login + Google OIDC via Spring Security (session cookie).
  A mobile-friendly bearer-token flow is not yet implemented (see
  INTERNAL_TODO.md).
- Live delivery: IMAP IDLE (`ImapIdleService`) + SSE
  (`/chats/events`, `ChatEventStream`).

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java   # main
├── admin/       # /admin/revenue, weekly digest, win-back
├── api/         # JSON REST controllers (mobile-facing) — NEW, added 2026-08-29
├── auth/        # Spring Security, users, sessions, Google OIDC
├── billing/     # Stripe checkout + webhook, plan gating
├── digest/      # Weekly digest emails (subscriber-facing)
├── domain/      # JPA @Entities (Message, EmailThread, Participant, …)
├── email/       # IMAP/SMTP: MailAccountService, MailboxPollingService,
│                #   ImapIdleService, EmailImportService
├── reengagement/# Activation, trial-end, dormant nudges
├── repository/  # Spring Data repositories
├── service/     # Domain-level services: conversation building, IM
│                #   transform, reply orchestration
├── team/        # Teams and invites
└── web/         # Thymeleaf controllers, view models, SSE events
```

## Key services (shared between web + API)

- `ConversationListService` (`web/`) — assembles the chats list rows for
  a user, with optional per-mailbox scope, free-text query, and triage
  filter. **Both** the Thymeleaf `/chats` view and the JSON
  `/api/v1/chats` endpoint go through this service; there is no
  duplicate query path.
- `ChatService` (`web/`) — assembles a single conversation's unified
  timeline. To be shared with a future `/api/v1/chats/{key}` endpoint.
- `ReplyService` (`service/`) — creates outbound `Message` rows and
  threads them onto the correct `EmailThread` (currently always the
  thread's last message — see roadmap in `../CLAUDE.md`).
- `OutboundMessageService` (`web/`) — hands the message to SMTP.

## Domain highlights

- `EmailThread` groups messages by `Message-ID` / `In-Reply-To` /
  `References`.
- A `Message` has a `conversationKey` — the sorted, normalized
  participant set — so multiple threads with the same people collapse
  into one "chat" in the UI, matching iMessage semantics.
- `Participant`s are deduped by lowercased email; display name is
  best-effort from headers or parsed signatures.
- `MailAccount` stores the user's connected mailboxes (IMAP password
  encrypted with `MAILBOX_ENCRYPTION_PASSWORD`; Gmail OAuth refresh
  token stored per account).

## How website and API stay consistent

- Business rules live in `service/*` and `web/*Service.java`, never in
  controllers or Thymeleaf templates.
- The Thymeleaf controllers (`web/*Controller.java`) render HTML from
  these services.
- The REST controllers (`api/*Controller.java`) return JSON DTOs mapped
  from the SAME service results, one small mapper per endpoint. See
  `docs/API.md` for the wire format.

## Building and running

- Build: `./mvnw clean package`
- Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `./mvnw test`
- One test: `./mvnw test -Dtest=ApiChatsControllerTest`
