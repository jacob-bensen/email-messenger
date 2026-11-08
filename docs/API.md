# API contract

**Status: no mobile REST API is implemented.** `/api/v1` below is PLANNED (TBD) and is
the target contract for the Flutter client. Everything under "Existing" is real code.

## Existing JSON/stream endpoints (session-cookie auth, CSRF token required on POST)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session | `200 {"imported": <int>}` — polls the user's active mailboxes now |
| GET | `/chats/events` | session | `text/event-stream` (SSE) — live new-mail events (`ChatEventsController`, `NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature | Stripe webhook; CSRF-exempt |

All other routes are HTML (see `WEB.md`).

## Planned `/api/v1` (TBD — not implemented; shapes are a proposal, not a contract yet)
Auth: bearer token (mechanism TBD). Errors: `{ "error": "<code>", "message": "<text>" }`
with 401/403/404/409/422/429; plan-limit failures reuse `PlanLimitExceededException` -> 403 `plan_limit`.

- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` — TBD
- `GET /api/v1/chats` — list conversations (maps to `ConversationListService`)
- `GET /api/v1/chats/{key}` — bubble runs (maps to `ChatService`)
- `POST /api/v1/chats/{key}/send` — reply (maps to `OutboundMessageService`)
- `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/{id}/sync` — TBD

The first implemented endpoint must be written here with full request/response schemas first.
