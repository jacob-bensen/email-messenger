# API contract (canonical)

**Status (2026-10-27): no mobile REST API exists.** The server exposes only session-cookie + CSRF protected, server-rendered routes (see `docs/WEB.md`) and three non-HTML endpoints listed below. The mobile client repo (`conexus-mail-mobile`) is empty, so there is no client/server mismatch yet — only a missing contract.

## Existing non-HTML endpoints (as implemented)

### `POST /chats/refresh`
Auth: session cookie + CSRF token. Synchronously polls the user's mailboxes.
Response `200`: `{"imported": <int>}`. Errors: `302 /login` when unauthenticated, `403` on bad CSRF.

### `GET /chats/events` (SSE)
Auth: session cookie. `text/event-stream` of new-mail events (`NewMailEvent`); payload schema **TBD** (read `ChatEventStream`/`NewMailEvent` before documenting).

### `POST /billing/webhook`
Auth: Stripe signature header; CSRF-exempt. Not for clients.

## Planned `/api/v1` (not implemented)

The first feature run should define, in this file *before* code: token auth (login → bearer token, refresh, revoke), `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`, error envelope `{ "error": { "code", "message" } }`. Each must call the existing services (`ConversationListService`, `ChatService`, `OutboundMessageService`) — not the controllers.
