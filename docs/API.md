# API — ConexusMail

Canonical contract between the backend (this repo) and any programmatic
consumer, including the sibling `conexus-mail-mobile` Flutter client.

Status: **there is essentially no mobile-facing REST API yet.** The
current backend is a Thymeleaf web app plus one SSE stream, one JSON
heartbeat, a Stripe webhook, and PWA/SEO assets. Building a mobile client
requires either (a) reusing the session cookie against HTML endpoints
(fragile, CSRF-heavy), or (b) introducing a new JSON API layer under
`/api/v1/**` that reuses the existing service beans. Option (b) is the
intended long-term direction and is tracked in `../INTERNAL_TODO.md`.

The full HTML surface — routes, methods, view names, auth — is documented
separately in `WEB.md`.

---

## 1. Machine-readable endpoints that exist today

### 1.1 `GET /chats/events` — Server-Sent Events (SSE)

Live new-mail stream for the signed-in user.

- Content-Type: `text/event-stream`
- Auth: session cookie (browser tab). Emits are per-user.
- Backed by `ChatEventsController` → `ChatEventStream`.
- Events are published by `MailboxPollingService` after every poll round
  and by `ImapIdleService` when IMAP IDLE fires (paid tier only, guarded
  by `mailbox.idle.enabled`).
- Event payload shape (as of V32 conversation-key threading): a JSON blob
  containing at least the affected `conversationKey` so the client can
  reconcile without a full reload. Exact field names — TBD, document them
  precisely as part of the first mobile spike.

### 1.2 `POST /chats/refresh` — refresh heartbeat

- Content-Type: `application/json`
- Response: `{"imported": <int>}` — count of newly imported messages this
  round.
- Auth: session cookie.
- Backed by `ConversationController#refresh`, annotated `@ResponseBody`.

### 1.3 `POST /billing/webhook` — Stripe webhook

- Server-to-server only. Not a mobile-client endpoint.
- Verifies `Stripe-Signature` header against `STRIPE_WEBHOOK_SECRET`.
- CSRF-exempted; publicly reachable.
- Backed by `BillingWebhookController` (`@RestController`).

### 1.4 PWA and SEO assets

Public, unauthenticated, non-view endpoints served as raw bytes:

- `GET /manifest.webmanifest` — PWA manifest (JSON)
- `GET /sw.js` — service worker
- `GET /offline` — offline shell (HTML)
- `GET /icons/icon-192.png`, `/icons/icon-512.png`,
  `/icons/icon-512-maskable.png`, `/apple-touch-icon.png`
- `GET /robots.txt`, `/sitemap.xml`
- `GET /images/og-card.png`

These are not part of the mobile-client contract; they exist for browsers
and crawlers.

---

## 2. Proposed mobile API (NOT YET IMPLEMENTED)

Sketch only — do not treat as a contract until endpoints exist. The Flutter
client's `lib/domain/` model must mirror whatever the backend actually
ships. Every entry below will land as its own Flyway-free (no schema needed)
`@RestController` under a new package `com.emailmessenger.api.v1` that
delegates to the existing service beans.

Common conventions once we start implementing:

- Base path: `/api/v1`
- Auth: bearer token in `Authorization: Bearer <token>`. Token minting
  endpoint (`POST /api/v1/auth/token`) accepts either
  email+password or a Google ID token; returns access + refresh tokens.
- Content-Type: `application/json; charset=utf-8` for requests and
  responses; error bodies always
  `{"error": {"code": "...", "message": "..."}}`.
- Errors: 400 validation, 401 no/expired token, 403 wrong user, 404 not
  found, 409 conflict, 429 rate-limited, 5xx server. Every mutation is
  wrapped in a single service-layer transaction so partial writes never
  leak.
- Pagination: cursor-based, `?cursor=<opaque>&limit=<n≤100>`; response
  carries `nextCursor` (null when exhausted).
- Times: ISO-8601 UTC (`2026-08-30T14:22:11Z`).
- IDs: opaque strings; do not encode DB row shape.

Endpoint sketch (to be turned into real specs when each is built):

| Method | Path                             | Purpose                                        |
| ------ | -------------------------------- | ---------------------------------------------- |
| POST   | `/api/v1/auth/token`             | Password or Google ID token → access + refresh |
| POST   | `/api/v1/auth/refresh`           | Refresh token → new access token               |
| GET    | `/api/v1/me`                     | Current user + plan                            |
| GET    | `/api/v1/mailboxes`              | List connected mailboxes                       |
| POST   | `/api/v1/mailboxes`              | Connect an IMAP mailbox                        |
| DELETE | `/api/v1/mailboxes/{id}`         | Disconnect                                     |
| POST   | `/api/v1/mailboxes/{id}/sync`    | Force a sync                                   |
| GET    | `/api/v1/chats`                  | List chats (conversationKey rail)              |
| GET    | `/api/v1/chats/{key}`            | Timeline of one chat                           |
| POST   | `/api/v1/chats/{key}/send`       | Reply / send new message                       |
| POST   | `/api/v1/chats`                  | Start a brand-new chat (compose)               |
| GET    | `/api/v1/searches`               | List saved searches                            |
| POST   | `/api/v1/searches`               | Create a saved search                          |
| DELETE | `/api/v1/searches/{id}`          | Delete a saved search                          |
| GET    | `/api/v1/events`                 | SSE stream (mirror of `/chats/events`)         |

The mobile client should treat the backend as authoritative for every
computed field (unread count, delivery state, thread merges): predict
optimistically, then reconcile from the server's response — on conflict,
server wins.

Every row above is TBD until the corresponding controller is implemented
and its DTOs are documented in this file. Do not build against the sketch.
