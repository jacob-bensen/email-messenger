# API contract (canonical)

Status: **no mobile REST API exists.** This file records what machine-readable
endpoints exist today and reserves the planned surface. Anything under
"Planned" is TBD and must be defined here *before* implementation.

## Existing non-HTML endpoints
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/chats/events` | session | SSE stream (`text/event-stream`) of new-mail events for the user; consumed by the web chat for in-place refresh. |
| POST | `/chats/refresh` | session + CSRF | Triggers an immediate poll. Response `200 {"imported": <int>}`. |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe events. Not for clients. |

All other routes return HTML (see `WEB.md`).

## Planned (TBD — not implemented)
- Auth: `POST /api/v1/auth/token` (email+password → bearer token), refresh, revoke.
- `GET /api/v1/chats?filter=&q=&mailbox=&page=` — conversation list (mirrors `ConversationListService`).
- `GET /api/v1/chats/{key}` — timeline (bubble runs from `ChatService`).
- `POST /api/v1/chats/{key}/send` — mirrors `OutboundMessageService`.
- `GET /api/v1/mailboxes`.
- Error envelope: `{ "error": "<code>", "message": "<text>" }`, codes TBD (plan-limit exceeded must be distinct, cf. `PlanLimitExceededException`).
