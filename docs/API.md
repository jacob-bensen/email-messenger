# API contract (canonical)

Status: **no mobile REST API is implemented yet.** This file documents the JSON/
stream endpoints that exist today and the planned `/api/v1` surface (marked
PLANNED — not implemented; do not code clients against it until promoted here).
Both repos must match this file; update it BEFORE implementing.

## Implemented non-HTML endpoints

All require the session cookie unless noted; CSRF token required on POST
(header `X-CSRF-TOKEN` or `_csrf` param).

### POST /chats/refresh
Polls the user's active mailboxes immediately.
- 200 `{"imported": <int>}`
- 302 → `/login` if unauthenticated.

### GET /chats/events  (SSE, `text/event-stream`)
Per-user stream of new-mail notifications (`NewMailEvent`, published by
`MailboxPollingService`). Event payload shape: TBD — read `ChatEventStream` /
`NewMailEvent` before relying on it.

### POST /billing/webhook  (public, Stripe-signed, CSRF-exempt)
Stripe webhook; header `Stripe-Signature`. Invalid signature → 400.

## PLANNED /api/v1 (not implemented)
Goal: let the mobile client read conversations and reply, reusing
`ConversationListService`, `ChatService`, `ReplyService`/`OutboundMessageService`.
- Auth: `POST /api/v1/auth/login` → bearer token (TBD design; needs a new
  Flyway table for API tokens).
- `GET /api/v1/chats` — conversation list.
- `GET /api/v1/chats/{key}` — bubbles for a conversationKey.
- `POST /api/v1/chats/{key}/send` — reply.
Schemas: TBD at implementation time.

## Error codes
HTML endpoints render `error.html` via `GlobalExceptionHandler`
(413 for oversized uploads; plan-limit violations via
`PlanLimitExceededException`). JSON error envelope: TBD.
