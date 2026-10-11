# API.md — canonical API contract

**Status (2026-10-11): no mobile-facing REST API exists yet.** The mobile client
has nothing to call. This file documents what exists and the contract
conventions the first `/api/v1` feature must follow. Never document behavior
that is not implemented as if it were; planned items are marked PLANNED.

## Existing non-HTML endpoints (session-cookie auth, CSRF required for POST)
### `POST /chats/refresh`
Auth: session. Triggers an immediate import for the caller's active mailboxes.
Response `200 application/json`: `{"imported": <int>}`.

### `GET /chats/events`
Auth: session. `text/event-stream`; event name `new-mail` published when the
poller/IDLE imports mail for the user (30 min emitter timeout, client reconnects).

## PLANNED conventions for `/api/v1` (TBD, not implemented)
- Auth: TBD (bearer token issued by a new login endpoint; session+CSRF is not
  suitable for a native client).
- JSON, ISO-8601 UTC timestamps, error body `{"error": "<code>", "message": "..."}`.
- Resources mirror services: conversations (keyed by `conversationKey`), messages,
  send/reply, mailboxes.
- All endpoints must call the same services as the Thymeleaf controllers.
