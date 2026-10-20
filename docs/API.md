# API contract (canonical)

**Status (2026-10-20): there is no REST API for the mobile client.** The server is
session-cookie + CSRF + server-rendered HTML. This file records what exists and
the contract slice that must be defined (contract-first) before the client can be
built. Anything marked TBD is not implemented.

## Existing non-HTML endpoints

| Method / path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 application/json` `{"imported": <int>}` — synchronously polls the caller's mailboxes |
| `GET /chats/events` | session | none | `text/event-stream` (SSE); new-mail events (`NewMailEvent`), payload shape TBD (see `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event JSON | `200` / `400` on bad signature |

Errors for all other routes are HTML (`error.html`) or redirects; no JSON error envelope exists (TBD).

## Needed for the mobile client (TBD — not implemented)

1. Token auth (login, refresh, logout) — current auth is cookie+CSRF only.
2. `GET /api/v1/conversations` (list, paged, `mailbox`/`q`/`filter`), `GET /api/v1/conversations/{key}` (bubble runs), `POST /api/v1/conversations/{key}/messages` (send, mirrors `/chats/{key}/send`: `body`, `subject`, `asReply`, attachments), `POST /api/v1/sync`.
3. JSON error envelope `{code, message}`; plan-limit error mapping (`PlanLimitExceededException`).
4. All of the above must delegate to the same services as the Thymeleaf controllers (first extract the send/list logic now inlined in `ConversationController`).
