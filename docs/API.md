# API contract (canonical)

## Status: NO MOBILE REST API EXISTS YET (audited 2026-11-08)

The backend only exposes server-rendered, session+CSRF web routes (`WEB.md`).
Two endpoints return non-HTML and are the only machine-readable surface:

### `POST /chats/refresh`
Auth: session + CSRF. Response `200 {"imported": <int>}` — number of newly
imported messages after a synchronous poll of the user's mailboxes.

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE); emits `NewMailEvent`s (payload shape: TBD,
see `ChatEventStream`).

### `POST /billing/webhook`
Stripe-only (signature header); not for clients.

## Planned (not implemented) — `/api/v1`
Proposed first slice, to be defined in full here BEFORE implementation:
1. Token auth for mobile (`POST /api/v1/auth/login` → bearer token; needs a
   stateless filter chain separate from the web chain, CSRF-exempt).
2. `GET /api/v1/chats` (list, paged, filter/q), `GET /api/v1/chats/{key}`
   (bubble runs), `POST /api/v1/chats/{key}/messages` (reply).
3. `GET /api/v1/mailboxes`.
All must call the same services as the web controllers. Error envelope: TBD.
