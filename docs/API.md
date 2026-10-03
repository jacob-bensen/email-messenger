# API contract

**Status (2026-10-03): there is no REST API for the mobile client.** The app is
server-rendered with session-cookie + CSRF auth. This file is the canonical
contract; the mobile client MUST NOT call anything not listed under "Defined".

## Defined (implemented today, JSON/stream)

### `POST /chats/refresh`
Auth: session + CSRF token. Request: empty body. Response `200`:
`{"imported": <int>}` — count of newly imported messages (synchronous sync).
### `GET /chats/events`
Auth: session. `text/event-stream` (SSE) of new-mail events for the user
(`ChatEventStream`). Event payload schema: TBD (see `NewMailEvent`).
### `POST /billing/webhook`
Stripe-only; signature-verified; not for clients.

Errors: HTML error page via `GlobalExceptionHandler`; JSON error shape TBD.

## Planned `/api/v1` (NOT implemented — contract-first TBD)
Needed before any mobile feature: token auth (login → access/refresh token),
`GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`,
`POST /api/v1/conversations/{key}/messages`, `POST /api/v1/mailboxes/...`.
Each must be specified here before implementation and reuse `ChatService`,
`ConversationListService`, `OutboundMessageService`.
