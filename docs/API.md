# API contract (canonical)

**Status: no mobile REST API exists yet.** Nothing below `/api/**` is implemented. This file
documents what is actually exposed today and the gaps; any new endpoint must be added here
*before* implementation, and the client in `conexus-mail-mobile` must match it exactly.

## Existing JSON/stream endpoints (session-cookie auth, CSRF required except webhook)

### `POST /chats/refresh`
- Auth: session. Body: none (CSRF token header/param required).
- 200: `{"imported": <int>}` — newly imported messages for the caller's active mailboxes.

### `GET /chats/events`
- Auth: session. `text/event-stream` (SSE); emits "new mail" signals (`NewMailEvent`). Event payload shape: TBD (see `ChatEventStream`).

### `POST /billing/webhook`
- Public, Stripe signature header, CSRF-exempt. Server-to-server only; not for the client.

## Everything else
All other routes return server-rendered HTML (see `docs/WEB.md`). They are not a client contract.

## Required for the mobile client (TBD, not yet specified)
Token auth (login/refresh/logout), conversation list, conversation messages, send/reply,
mailbox list/connect/sync, push registration. Specify here first.

## Error codes
TBD — currently HTML error page (`error.html`) / Spring defaults.
