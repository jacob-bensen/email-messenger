# API.md — canonical API contract

**Status: no mobile REST API is implemented.** The sections below document the only
machine-readable endpoints that exist today (used by the web UI). Everything under
"Planned" is TBD and must be specified here *before* implementation.

Auth for all endpoints below: authenticated browser session (cookie) + CSRF token header/param on POSTs.
Errors: HTML error page (`error.html`) via `GlobalExceptionHandler`; no JSON error envelope exists (TBD).

## Existing JSON/stream endpoints
### POST /chats/refresh
Synchronously polls the caller's active mailboxes.
Response `200 application/json`: `{"imported": <int>}` (new messages imported).

### GET /chats/events
`text/event-stream` (SSE). One stream per user (`ChatEventStream.subscribe(userId)`). Emits an event when `NewMailEvent(userId, importedCount)` fires. Exact event name/payload: TBD (read `ChatEventStream`).

### POST /billing/webhook
Stripe webhook; public, signature-checked via `Stripe-Signature` header. `400` on missing/invalid signature. Not for clients.

## Planned (TBD, not implemented)
Token auth (`/api/v1/auth/*`), conversation list/detail, send reply, mailbox list, push-token registration. Suggested rule: controllers under `/api/v1` call `ChatService` / `ReplyService` / `ConversationListService` — the same services the Thymeleaf pages use.
