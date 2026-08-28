# API contract

**Status: no mobile JSON API exists yet.** This file is the canonical
contract between the backend and the sibling `conexus-mail-mobile`
Flutter client — it is spec-first, so nothing should ship on either
side without landing here first. As of 2026-08-28 the surface below is
almost entirely "planned"; the "existing" section describes what is
actually implemented today.

## Existing HTTP surface (as of 2026-08-28)

The backend is a Thymeleaf server-rendered app plus one JSON endpoint.
The full list of controller mappings lives across
`src/main/java/com/emailmessenger/**/*Controller.java`; the shape of
what returns JSON today is small enough to enumerate here:

### JSON endpoints

- **`POST /billing/webhook`** — Stripe webhook receiver.
  - `Content-Type: application/json`, body is a Stripe event envelope.
  - `Stripe-Signature: <sig>` header is required and verified via
    `StripeWebhookGateway`.
  - Responds `200 OK` (empty body) on accepted events. Any of
    `checkout.session.completed`,
    `customer.subscription.{created,updated,deleted}` is mirrored onto
    a local `Subscription` row. Everything else is ignored.
  - `400` for a missing/invalid signature or malformed body.
  - Not part of the mobile API contract — Stripe calls this directly.

### SSE endpoints (browser-only today)

- **`GET /chats/events`** — Server-Sent Events stream of
  `NewMailEvent`s scoped to the authenticated user.
  - `Accept: text/event-stream`.
  - Event `data:` payload is a small JSON blob (`conversationKey`,
    `threadId`, `messageId`); the payload shape lives with
    `web/ChatEventStream`.
  - Consumed by `fragments/live-refresh.html` to morph the chat pane
    in place. A mobile client could subscribe once the auth story
    supports non-cookie sessions (or once the mobile carries the
    session cookie set by a JSON auth endpoint that does not yet
    exist).

### HTML endpoints

Everything else — auth (`/login`, `/register`, `/auth/google/start`,
`/password/*`, `/verify-email*`), account (`/account`,
`/account/appearance`, `/account/notifications`, `/account/privacy`,
`/account/password`, `/account/email`), mailboxes (`/mailboxes*`,
`/mailboxes/gmail/*`), chats (`/chats`, `/chats/{key}`,
`/chats/{key}/pane`, `/chats/{key}/send`, `/chats/new`,
`/chats/refresh`), billing pages (`/billing/checkout`,
`/billing/portal`, `/billing/success`, `/billing/cancel*`), team
invites, admin dashboard, saved searches, PWA manifest/service-worker,
SEO (`/robots.txt`, `/sitemap.xml`, `/images/og-card.png`), digest
opt-out — is Thymeleaf HTML returning either a full page or an HTML
fragment (chat pane, live-refresh partial). None of it is designed for
a JSON client. `docs/WEB.md` enumerates the pages.

## Planned mobile API

None of this exists yet. When any of it is implemented, the endpoint
below moves out of "planned" into a live table with the exact request
and response schemas locked down.

### Conventions (proposed, subject to review before the first endpoint ships)

- Prefix: `/api/v1/…`.
- Auth: cookie session for now (same session the web uses), so the
  auth endpoint issues `Set-Cookie: JSESSIONID=…`. A bearer-token
  flow can come later without breaking the shape of other endpoints.
- Content types: `application/json` request and response.
- Errors: RFC 7807 (`application/problem+json`) with `type`, `title`,
  `status`, `detail`, and an app-specific `code`.
- Time: ISO-8601 with offset (`2026-08-28T12:34:56Z`).
- IDs: server-assigned; the client never invents primary keys.
- Pagination: cursor-based (`?after=<opaqueCursor>&limit=50`) so a
  live-updating chats list is stable while new mail arrives.

### Endpoints (planned, unimplemented)

| Method | Path                        | Purpose                                                    |
| ------ | --------------------------- | ---------------------------------------------------------- |
| POST   | `/api/v1/auth/login`        | Email + password → session cookie.                         |
| POST   | `/api/v1/auth/logout`       | Invalidate session.                                        |
| GET    | `/api/v1/auth/me`           | Current user + plan + billing summary.                     |
| GET    | `/api/v1/mailboxes`         | List connected mailboxes for the current user.             |
| POST   | `/api/v1/mailboxes`         | Add an IMAP-password mailbox.                              |
| DELETE | `/api/v1/mailboxes/{id}`    | Disconnect a mailbox.                                      |
| GET    | `/api/v1/chats`             | Chats list keyed by `conversationKey`, newest first, cursor. |
| GET    | `/api/v1/chats/{key}`       | Full chat: bubbles, senders, day separators.               |
| POST   | `/api/v1/chats/{key}/reply` | Send an inline reply; threads via the chat's last message. |
| POST   | `/api/v1/chats/{key}/read`  | Mark a chat read up to `messageId`.                        |
| GET    | `/api/v1/events`            | SSE stream — same events as `/chats/events` today.         |
| GET    | `/api/v1/preferences`       | Notification / appearance / digest preferences.            |
| PUT    | `/api/v1/preferences`       | Update preferences.                                        |
| POST   | `/api/v1/push/subscribe`    | Register a device for push (once Web Push ships).          |

### DTO shapes (planned)

To be defined alongside each endpoint. The mobile client is expected
to hold matching Dart DTOs in `lib/data/dto/` and to derive the same
`conversationKey` normalization the server uses (see
`service/ConversationKeyService`) so optimistic bubbles match what the
server returns.

## Change rules

- No endpoint ships on either side without first updating this file
  in the same commit / run.
- Once an endpoint is implemented on the server AND consumed by a
  client, its shape is frozen; changes go in a new versioned path
  (`/api/v2/…`) or an additive-only field.
