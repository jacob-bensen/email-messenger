# API.md — canonical contract

**Status (2026-10-10): no mobile REST API is implemented.** This file documents only what actually exists and
marks the planned mobile surface as TBD. Do not implement client calls against TBD sections until they are
specified here and built on the server in the same run.

## Auth for all endpoints below
Session cookie (`JSESSIONID`) from `POST /login` (form-encoded `email`, `password`, plus CSRF token `_csrf`).
Unauthenticated requests get a 302 to `/login`.

## Existing machine-readable endpoints
### POST /chats/refresh
Auth: session + CSRF. Synchronously polls the caller's mailboxes.
Response 200 `application/json`: `{"imported": <int>}` — newly imported message count.

### GET /chats/events  (SSE, `text/event-stream`)
Auth: session. Emits event `new-mail` for the caller (`NewMailEvent`): `data: {"imported": <int>}` ; periodic `: ping` comments.


### POST /billing/webhook
Stripe → server only. Public, CSRF-exempt, verified by Stripe signature. Not for clients.

## Planned mobile API (TBD — not implemented)
Needs: token auth (e.g. `POST /api/v1/auth/token`), `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`
(bubble runs from `ChatConversation`/`BubbleRun`), `POST /api/v1/conversations/{key}/send`, push registration.
Errors: TBD (web uses `GlobalExceptionHandler` → HTML `error.html`).
