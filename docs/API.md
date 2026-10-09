# API contract

**Status: no mobile REST API exists yet.** The server is a session-cookie, CSRF-protected,
server-rendered website. This file is the canonical contract; it currently lists only the
non-HTML endpoints that exist, then the *planned* surface (marked TBD — not implemented,
not to be assumed by clients).

## Existing non-HTML endpoints

### `POST /chats/refresh`
Auth: session cookie + CSRF token. Triggers a synchronous mailbox refresh for the caller.
Response `200 application/json`: `{"imported": <int>}` (newly imported messages).

### `GET /chats/events`
Auth: session cookie. `text/event-stream` (SSE) of new-mail signals (`NewMailEvent`) for the
caller. Event payload schema: TBD (see `ChatEventStream`).

### `POST /billing/webhook`
Auth: none; `Stripe-Signature` header verified. Body: raw Stripe event JSON.
`200 "ok"`; `400` missing/invalid signature; `503` webhook not configured; `500` apply failure (Stripe retries).

## Planned (TBD — blocked on design in the mobile repo's `docs/DESIGN.md`)
- Token auth for mobile (login → bearer token / refresh), exempt from CSRF.
- `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`,
  mailbox list/connect, plan/limits. Shapes to be defined here **before** implementation.
- Error envelope: TBD (website uses `GlobalExceptionHandler` + `error.html`).
