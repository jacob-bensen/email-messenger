# API CONTRACT (canonical)

Status as of 2026-10-31: **no mobile REST API exists yet.** The only JSON/machine endpoints are below.
Any new mobile endpoint MUST be added here first (contract-first) and implemented on both sides in one run.

## Proposed conventions for the future mobile API (TBD, not implemented)
- Base path `/api/v1`, JSON, Bearer-token auth (needs design: session+CSRF is unsuitable for mobile).
- Error body: `{ "error": "<code>", "message": "<text>" }`.
- Reuse service layer (`ChatService`, `ConversationListService`, `ReplyService`, ...); no logic in controllers.

## Existing machine-facing endpoints

### POST /chats/refresh
Auth: session + CSRF token. Synchronously polls the user's active mailboxes.
Response 200: `{ "imported": <int> }`.

### GET /chats/events
Auth: session. `text/event-stream` (SSE) of new-mail notifications (`NewMailEvent`) for the user.
Event payload schema: TBD (see `ChatEventStream`).

### POST /billing/webhook
Auth: none; `Stripe-Signature` header verified. CSRF-exempt. Body: raw Stripe event JSON.
Responses: 200 `ok`; 400 `missing Stripe-Signature header` | `invalid signature`; 503 `webhook not configured`;
500 `error` (Stripe retries).

## Candidate first mobile feature (not started)
Token login + `GET /api/v1/conversations` (list, wraps `ConversationListService`) +
`GET /api/v1/conversations/{key}` (wraps `ChatService`) + `POST /api/v1/conversations/{key}/send` (wraps `ReplyService`).
