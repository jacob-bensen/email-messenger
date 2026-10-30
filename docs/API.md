# API contract

**Status (2026-10-30): no mobile REST API is implemented.** This file is the
canonical contract; section "Implemented today" lists what actually exists,
"Planned v1" is the target for the Flutter client and is NOT yet served.
Both repos must change together when this file changes.

## Implemented today (non-HTML endpoints)

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | SSE stream of new-mail events (payload shape TBD — see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe only; not for clients |
| GET | `/actuator/health` | none | Spring Boot health JSON |

Errors: HTML error page (`error.html`) for browser requests; JSON error shape TBD.

## Planned v1 (`/api/v1`, JSON, bearer-token auth) — TBD, not implemented

Principles: server authoritative; reuse the web service layer; CSRF-exempt
because auth is `Authorization: Bearer`. Proposed first slice (see
`INTERNAL_TODO.md` #1):

- `POST /api/v1/auth/login` `{email,password}` → `{accessToken, refreshToken, expiresIn}`
- `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- `GET /api/v1/chats?q&mailbox&filter&page` → paged conversation list
- `GET /api/v1/chats/{key}` → messages as bubble runs
- `POST /api/v1/chats/{key}/send`
- Error body: `{"error": "<code>", "message": "<text>"}` with 401/403/404/409/422.

Exact schemas are TBD until the slice is built; they must be written here first.
