# API.md — canonical API contract

**Status (2026-10-26): no mobile REST API exists yet.** This file documents
the HTTP surface that actually exists, then the status of the mobile API.
Both repos depend on it; update it BEFORE changing any endpoint.

## Conventions today
- Auth: session cookie (`JSESSIONID`) from `POST /login` (form fields
  `email`, `password`, optional `remember-me`) + CSRF token on every POST.
- Errors: HTML error page (`error.html`) via `GlobalExceptionHandler`.

## JSON / streaming endpoints that exist
### `POST /chats/refresh`
Auth: session + CSRF. Synchronously polls the user's active mailboxes.
Response `200 application/json`: `{"imported": <int>}`.

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE) published by `ChatEventStream`;
events originate from `NewMailEvent`. Event payload schema: TBD (see
`ChatEventStream`).

### `POST /billing/webhook`
Stripe webhook. No session, CSRF-exempt, signature-verified
(`InvalidStripeSignatureException` on failure). Not for clients.

## Mobile REST API (`/api/v1/**`)
**TBD — not implemented.** The first run that implements it must: choose a
token auth scheme (session+CSRF is unsuitable for a native client), define
conversation list / conversation / send / mailbox endpoints in this file,
and have controllers call the same services as the Thymeleaf pages
(`ConversationListService`, `ChatService`, `OutboundMessageService`).
