# API contract (canonical)

Status as of 2026-10-02: **no mobile REST API exists.** There is no `/api/**`
namespace, no token auth, and no `@RestController` apart from the Stripe
webhook. The mobile repo (`conexus-mail-mobile`) is empty. This file records
(1) what machine-readable endpoints exist and (2) the contract that new work
must extend. Anything marked TBD is not implemented.

## Conventions (proposed for v1, TBD until implemented)
- Base path `/api/v1`, JSON, UTF-8, ISO-8601 UTC timestamps.
- Auth: TBD (bearer token issued by a new login endpoint). Session+CSRF
  (current web auth) is unsuitable for a native client.
- Errors: TBD; recommended RFC 7807 `application/problem+json`.

## Existing machine-readable endpoints

### POST /chats/refresh
Auth: session + CSRF. Imports new mail for the user's active mailboxes now.
Response 200: `{"imported": <int>}`.

### GET /chats/events
Auth: session. `text/event-stream` (SSE) of new-mail events (`NewMailEvent`
via `ChatEventStream`). Event payload schema: TBD (read `ChatEventStream`).

### POST /billing/webhook
Stripe → server only. Raw JSON + `Stripe-Signature`. Not for clients.

## Planned (not implemented)
Auth token, conversation list, conversation messages, send/reply, mailbox
list. First slice to specify when implemented: token login + `GET
/api/v1/chats` backed by `ConversationListService.list(...)`.
