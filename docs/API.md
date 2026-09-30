# API contract (canonical)

**Status 2026-09-30: no versioned REST API exists.** The mobile client (`conexus-mail-mobile`) has nothing
to call yet. This file documents the only machine-consumed endpoints that exist today and the gap.
Nothing below "Planned" is implemented; do not code against it.

## Existing JSON/SSE endpoints (session cookie + CSRF, website use only)

### `POST /chats/refresh`
Auth: session; CSRF token required. Body: none.
200: `{"imported": <int>}` — messages newly imported by a synchronous poll of the user's active mailboxes.
401/302 to `/login` when unauthenticated.

### `GET /chats/events`
Auth: session. `text/event-stream`. Event `new-mail` with data `{"imported": <int>}`; `: ping` comment heartbeats.

## Planned (TBD — needs a design decision)
- Token auth for mobile (e.g. `POST /api/v1/auth/token`, bearer JWT/opaque token, separate stateless filter chain
  with CSRF off for `/api/**`).
- `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`,
  reusing `ChatService`/`ConversationListService`/`ReplyService`.
- Error envelope: TBD.
