# API contract (canonical)

Both repos depend on this file. **Status: the server exposes no REST/JSON API for
the mobile client yet.** Only these machine-facing endpoints exist today:

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | returns `{"imported": <int>}` — web heartbeat |
| GET | `/chats/events` | session | SSE stream of new-mail signals (`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature | Stripe events; no JSON body contract for clients |
| GET | `/actuator/health` | public | Spring Actuator health |

## Planned (TBD — not implemented)
Token auth (`POST /api/v1/auth/token`), `GET /api/v1/conversations`,
`GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`,
`GET /api/v1/mailboxes`. Define request/response schemas here **before**
implementing, backed by `ChatService` / `ConversationListService` /
`OutboundMessageService`.

## Conventions to adopt
- Base path `/api/v1`, JSON, error body `{"error": "<code>", "message": "..."}`.
- Plan-limit failures map from `PlanLimitExceededException` to HTTP 402/403 (TBD).
