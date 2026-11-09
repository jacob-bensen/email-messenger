# API contract (CANONICAL — shared with conexus-mail-mobile)

Status 2026-11-09: **no mobile REST API is implemented.** Only the endpoints
below exist and they are session-cookie/CSRF protected (see BACKEND.md). The
mobile client must NOT depend on anything not listed in "Planned" until it is
implemented here first.

## Existing JSON/stream endpoints
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream`; events published from `NewMailEvent` (event schema TBD) |

## Planned (TBD — not implemented)
Namespace `/api/v1`, bearer-token auth (login -> access/refresh token),
JSON errors `{ "error": "<code>", "message": "..." }`. Candidate first
resources: `POST /api/v1/auth/login`, `GET /api/v1/chats`,
`GET /api/v1/chats/{key}/messages`, `POST /api/v1/chats/{key}/send`.
Schemas to be defined contract-first in the run that implements them.
