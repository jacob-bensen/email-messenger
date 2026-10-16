# API.md — canonical API contract

**Status: no mobile REST API exists yet.** This file documents only machine-readable endpoints that actually exist
today, then the planned (TBD, NOT implemented) mobile surface. Update this file BEFORE implementing anything.
Verified against the code on 2026-10-16.

## Conventions today
- Auth: browser session cookie + CSRF token (`_csrf`/`X-CSRF-TOKEN`). No Bearer tokens.
- Errors: unauthenticated → `302 /login`; there is no JSON error envelope (`GlobalExceptionHandler` renders HTML `error.html`).

## Existing non-HTML endpoints
### `POST /chats/refresh`
Auth: session + CSRF. Synchronously polls the user's due mailboxes.
Response `200 application/json`: `{ "imported": <int> }`.

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE) of new-mail signals produced by `ChatEventStream` (`NewMailEvent`).
Event payload schema: TBD (read `ChatEventStream` before relying on it).

### `POST /billing/webhook`
Auth: none (Stripe signature header verified); CSRF-exempt. Stripe-only; not for clients.

## Planned mobile surface (TBD — not implemented)
Proposed prefix `/api/v1`, Bearer-token auth, JSON errors `{ "error": "<code>", "message": "..." }`:
`POST /api/v1/auth/login`, `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`,
`POST /api/v1/conversations/{key}/messages`, `GET /api/v1/mailboxes`. Schemas to be defined when the first
feature is chosen; they must reuse `ChatService` / `ConversationListService` / `OutboundMessageService`.
