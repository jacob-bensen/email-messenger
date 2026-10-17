# API contract (canonical)

**Status: no mobile REST API is implemented.** This file documents the only machine-consumable
endpoints that exist today and reserves the structure for the mobile API. Nothing below under
"Planned" exists in code; do not build a client against it until it is moved to "Implemented".

Conventions for future endpoints: JSON, prefix `/api/v1`, bearer-token auth (TBD), errors as
`{"error": "<code>", "message": "<text>"}`.

## Implemented
Auth for all of these is the browser session cookie; POSTs need a CSRF token (`X-CSRF-TOKEN`).

### POST /chats/refresh
Triggers an immediate mailbox poll for the user's active mailbox(es).
- 200 `{"imported": <int>}`; 302 to `/login` if unauthenticated; 403 on bad CSRF.

### GET /chats/events (SSE, `text/event-stream`)
Per-user stream of new-mail events published by `MailboxPollingService` (`NewMailEvent`).
Event payload shape: TBD (see `ChatEventStream`); undocumented.

### POST /billing/webhook
Stripe webhook; signature header verified; CSRF-exempt; not for clients.

## Planned (not implemented)
Token auth (login/refresh), conversation list, conversation messages, send/reply, mailbox list, push registration.
