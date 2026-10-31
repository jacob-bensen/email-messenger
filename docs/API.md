# API.md — canonical contract

**Status: no mobile REST API exists yet.** The only machine-readable endpoints today are listed below. Everything else is server-rendered HTML (see `WEB.md`) and must not be consumed by the mobile client. Anything not listed is TBD; add new endpoints here *before* implementing them.

Auth today: session cookie + CSRF token (`_csrf` form field or `X-CSRF-TOKEN` header). A stateless auth scheme for mobile (e.g. bearer tokens) is TBD — see `INTERNAL_TODO.md`.

## Existing non-HTML endpoints

### POST /chats/refresh
Auth: session + CSRF. Imports new mail for the user's active mailboxes synchronously.
Response 200 `application/json`: `{"imported": <int>}`.
Errors: unauthenticated → redirect to `/login` (302), missing CSRF → 403.

### GET /chats/events
Auth: session. `text/event-stream` (SSE). Emits new-mail events published by `MailboxPollingService` (`NewMailEvent`). Event name/payload schema: TBD (see `ChatEventStream`).

### POST /billing/webhook
Stripe → server only. Signature-verified, no CSRF, not for clients.

## Planned (not implemented)
Auth token endpoint, conversation list, conversation messages, send/reply — TBD.
