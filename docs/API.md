# API contract (canonical)

## Status: no mobile REST API exists yet

As of 2026-10-27 the backend exposes only server-rendered HTML (see `WEB.md`) plus two JSON
endpoints. The Flutter client (`conexus-mail-mobile`) has no code yet, so there is nothing to
mismatch — but also no contract. The first feature run must define `/api/v1/**` here *before*
implementing it.

## Existing JSON endpoints

### `POST /chats/refresh`
Auth: session cookie + CSRF token. Request: no body. Triggers a synchronous mailbox refresh.
Response `200`: `{"imported": <int>}`. Errors: `302 /login` if unauthenticated, `403` bad CSRF.

### `POST /billing/webhook`
Stripe-only; signature-verified (`Stripe-Signature`), CSRF-exempt. Not for clients.

### `GET /chats/events` (SSE)
Auth: session. `text/event-stream` of new-mail events (`ChatEventStream`). Event payload schema: TBD
(read `NewMailEvent` before documenting).

## Planned `/api/v1` (TBD — not implemented)
Prerequisite decisions: bearer-token auth (the web uses cookie sessions + CSRF), error envelope,
conversation list/detail DTOs reusing `ConversationService`/`ChatService`, send via `ReplyService`
/ `OutboundMessageService`. All must go through the same services the Thymeleaf controllers use.
