# API.md — canonical API contract

**Status: no mobile REST API exists yet.** This file documents only what the server exposes today
that is machine-consumable, and marks everything else TBD. Do not implement client code against
anything marked TBD until it is specified here first (contract-first rule).

## Auth (current)
Cookie session. `POST /login` (form-encoded: `email`, `password`, optional `remember-me`, `_csrf`) →
302 + `Set-Cookie: JSESSIONID`. All state-changing requests require a CSRF token (`_csrf` form field or
`X-CSRF-TOKEN` header) except `/billing/webhook`. Unauthenticated requests to protected paths get 302 → `/login`.
**TBD:** token auth for mobile (see INTERNAL_TODO.md #1).

## Existing machine-consumable endpoints

### POST /chats/refresh
Auth: session + CSRF. Synchronously polls the caller's mailboxes.
Response 200 `application/json`: `{"imported": <int>}` (count of newly imported messages).

### GET /chats/events
Auth: session. `text/event-stream` (SSE) of live-mail events for the caller (`ChatEventStream`,
published by `MailboxPollingService` as `NewMailEvent`). Event payload schema: **TBD** (read
`ChatEventStream`/`live-refresh.html` before relying on it).

### POST /billing/webhook
Auth: Stripe signature header (no session, no CSRF). Server-to-server only; not for the client.

### GET /actuator/health
Public liveness.

## Planned (TBD — not implemented)
`/api/v1/**` for: auth token issue/refresh, conversation list, conversation detail (bubble runs),
mark-read, send/reply, mailbox list/sync, push-subscription registration. Each needs request/response
schemas and error codes defined here before implementation.

## Error format
Currently HTML error pages (`error.html`) via `GlobalExceptionHandler`; JSON error envelope: **TBD**.
