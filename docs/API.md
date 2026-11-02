# API.md — canonical API contract

> **Status: no mobile REST API exists yet.** This file documents (1) the non-HTML endpoints that exist today and (2) the planned `/api/v1` surface, which is **TBD** and must be specified here *before* implementation (see `INTERNAL_TODO.md`). The mobile repo must not call anything not listed under "Implemented".

## Conventions (current)
- Auth: session cookie; CSRF token required on all non-GET except `/billing/webhook`.
- Errors: unauthenticated → 302 to `/login` (HTML), not JSON 401. Plan limits → `PlanLimitExceededException` rendered as upgrade modal by `GlobalExceptionHandler`. **No JSON error envelope defined (TBD).**

## Implemented (non-HTML)
### `POST /chats/refresh`
Auth: session + CSRF. Triggers a synchronous mailbox refresh for the caller.
Response `200 application/json`: `{"imported": <int>}`.

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE) of new-mail events for the caller (`ChatEventStream`, event payload shape: TBD — read `NewMailEvent`/`ChatEventStream` before relying on it).

### `POST /billing/webhook`
Auth: none; Stripe signature header. Server-to-server only.

## Planned `/api/v1` (TBD — not implemented, not for client use)
Needed for the mobile app: token auth (login/refresh), `GET /conversations`, `GET /conversations/{key}` (bubble runs), `POST /conversations/{key}/messages`, `POST /conversations`, mailboxes list, `GET /me` (plan + limits). All must delegate to the existing services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`, `PlanLimitService`).
