# INTERNAL_TODO.md — email-messenger

Running list of known gaps, tech debt, and deferred work. Consume from
and append to this every run. Ordered by severity within each section:
**contract breakage > logic drift > missing tests > style**.

## Contract mismatches (server ↔ mobile)

The mobile client (`conexus-mail-mobile`) is a greenfield scaffold; the
backend has no JSON API. Every endpoint in `docs/API.md` is `TBD`. This
is the dominant piece of work.

Highest-severity items:

- **No `/api/v1` at all.** Auth, mailboxes, chats, search, and live
  events all need JSON endpoints wired through the existing service
  layer. Start with **auth** (session bootstrap) — nothing else works
  without it.
- **No token-based auth.** Website uses form login + session cookie only.
  Need a token service (`auth.token.*`): access-token JWT (15 min) +
  opaque refresh token, endpoints under `/api/v1/auth/*`, and a
  security filter that accepts `Authorization: Bearer …` alongside the
  existing cookie filter (do not weaken cookie CSRF).
- **No JSON DTO layer.** View models under `service/` are Thymeleaf-shaped
  records (`ChatConversation`, `BubbleRun`, `ConversationListItem`, …).
  Add DTOs in a new `web.api.dto.*` package that mirror the shapes in
  `docs/API.md`; keep view models untouched.
- **SSE endpoint is cookie-only.** `/chats/events` uses session auth;
  the mobile equivalent (`/api/v1/events`) will need bearer auth (with
  a `?access_token=` query fallback for `EventSource`).

## Logic drift

- **`ReplyService` always threads onto the thread's last message.**
  Roadmap has "reply-to-a-specific-bubble" — the mobile spec should
  honor `inReplyToMessageId` when the endpoint lands.
- **`Attachment.blob_ref` is always null.** Mobile `attachments` in
  `ChatDetail` will be metadata-only until the blob store lands
  (roadmap item "Per-person shared files & links").
- **Google OAuth still in Testing mode.** Refresh tokens expire in 7
  days; mobile Gmail connect needs the same weekly-reconnect UX until
  publish (roadmap item exists).

## Missing tests

- None new; existing suite runs via `./mvnw test`.

## Style / cleanup

- `web/` package is large (30+ files). Consider a `web.chats.*`,
  `web.mailboxes.*`, `web.account.*` sub-split before adding an
  `web.api.*` peer package.
- `email/` package holds only `ConversationKeyBackfill`; the real mailbox
  polling code appears to live elsewhere — grep and consolidate.

## Doc debt

- `docs/BACKEND.md` marks `service.mailbox.*` as TBD — inventory the
  actual polling + IDLE + import classes and fill that in on the next
  run that touches them.
- `docs/API.md` §Change log is a single entry; keep adding rows as the
  contract evolves.

## Blocked / external

- Google OAuth consent screen publish — external decision.
- Support email — external decision (address + outbound-from).

## Recently closed

_None yet — this file was seeded on 2026-08-28._
