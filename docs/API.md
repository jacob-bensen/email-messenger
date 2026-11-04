# API contract (canonical)

**Status: no mobile REST API exists yet.** The backend is a server-rendered
Thymeleaf app with session-cookie auth and CSRF. This file records (1) the only
JSON/stream endpoints that exist today and (2) the contract convention future
endpoints must follow. Never document behavior here that isn't implemented
without marking it **PLANNED**.

## Conventions for new endpoints (PLANNED)
- Prefix `/api/v1`, JSON, bearer-token auth (design TBD), CSRF-exempt for bearer calls.
- Errors: `{ "error": "<code>", "message": "<human text>" }` with proper HTTP status.
- Controllers must call the same services the Thymeleaf controllers use.

## Existing JSON/stream endpoints

### `POST /chats/refresh`
Auth: session + CSRF token. Triggers an immediate poll of the user's active mailboxes.
Response `200`: `{ "imported": <int> }`.

### `GET /chats/events`
Auth: session. `text/event-stream`. Event `new-mail`, data `{"imported": <int>}`.
Emitter timeout 30 min; clients reconnect.

### `POST /billing/webhook`
Stripe-signed, public, CSRF-exempt. Not for clients.

## Errors today
`GlobalExceptionHandler` renders HTML error pages (404 for unknown/foreign ids,
413 for oversized uploads). No JSON error shape yet.
