# API contract (canonical)

Status as of 2026-10-23: **no versioned REST API exists yet.** The mobile client
(`conexus-mail-mobile`) cannot talk to the server until one is defined. This file
documents (1) the machine-readable endpoints that exist today and (2) the planned `/api/v1`
surface, which is **TBD**. Define the contract here BEFORE implementing.

## Conventions (proposed, not yet implemented)
- Base path `/api/v1`, JSON, UTF-8. Server is authoritative; clients never send computed values.
- Errors: `{ "error": "<code>", "message": "<human text>" }` with HTTP 400/401/403/404/409/429.
- Auth: TBD (needs token auth; current auth is cookie session + CSRF).

## Existing machine-readable endpoints (session cookie + CSRF token required)
### `POST /chats/refresh`
Auth: session. Triggers an immediate sync of the user's mailboxes.
Response 200: `{ "imported": <int> }`.

### `GET /chats/events`
Auth: session. `text/event-stream`. Event `new-mail`; payload is the serialized
`NewMailEvent` `{ "userId": <long>, "importedCount": <int> }` (exact wire payload: TBD, verify in `ChatEventStream`).

## Planned `/api/v1` (TBD — not implemented)
Minimum for a mobile client, mirroring web features:
- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
- `GET /api/v1/conversations` (list, keyed by `conversationKey`)
- `GET /api/v1/conversations/{key}` (bubble runs)
- `POST /api/v1/conversations/{key}/messages` (reply; mirrors `POST /chats/{key}/send`)
- `GET /api/v1/mailboxes`
Each must reuse `ConversationListService` / `ChatService` / `OutboundMessageService` /
`ReplyService`, not the controllers.
