# API.md — canonical API contract

**Status (2026-10-11): no mobile REST API exists yet.** This file documents (1) what the server really exposes that a non-browser client could use and (2) the *planned* `/api/v1` surface, clearly marked PLANNED. Do not implement the client against PLANNED sections until they move to "Implemented".

## 1. Implemented, JSON/stream endpoints
Both require a browser session cookie and (for POST) a CSRF token; they are not yet usable from a native client.

### POST /chats/refresh
Auth: session + CSRF. Triggers an immediate poll of the user's active mailboxes.
200 `{"imported": <int>}`.

### GET /chats/events  (SSE)
Auth: session. Event `new-mail`, data `{"imported": <int>}`; `: ping` comment keepalives.

All other routes return HTML (see WEB.md); they are not part of this contract.

## 2. PLANNED — `/api/v1` (TBD, schema not final)
Planned first slice (chosen as next feature, see INTERNAL_TODO #1):
- `POST /api/v1/auth/token` — email+password → bearer token. TBD
- `GET /api/v1/chats?page&q&mailbox&filter` — conversation list (from `ConversationListService`). TBD
- `GET /api/v1/chats/{key}` — bubble runs (from `ChatService`). TBD
- `POST /api/v1/chats/{key}/send` — reply. TBD
- Error envelope: `{ "error": "<code>", "message": "<text>" }`, codes TBD.
