# API contract (canonical)

**Status: no mobile REST API exists yet.** The server today exposes only a
server-rendered website (see `WEB.md`). The FRONTEND repo
(`conexus-mail-mobile`) is an empty Flutter shell, so there is no client/server
mismatch — only an absent contract. This file is the place the contract will be
defined *before* implementation (next run).

## Machine-facing endpoints that exist today

| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` | 302→/login if unauthenticated; 403 bad CSRF |
| GET | `/chats/events` | session | none | SSE stream of new-mail events (`NewMailEvent`); payload shape TBD | 302→/login |
| POST | `/billing/webhook` | Stripe signature header | raw Stripe JSON | `200 "ok"` | 400 missing/invalid signature; 503 not configured; 500 apply failure (Stripe retries) |
| GET | `/actuator/health` | public | — | Spring Actuator health | — |

These are web-internal, not a stable mobile contract.

## Planned (TBD, not implemented)
- `POST /api/v1/auth/token` (+ refresh/revoke) — bearer auth for the client.
- `GET /api/v1/chats`, `GET /api/v1/chats/{key}/messages`,
  `POST /api/v1/chats/{key}/messages`, `POST /api/v1/chats/{key}/read`.
- Error envelope `{ "error": {"code","message"} }`.
All must delegate to the existing service layer (`ChatService` etc.).
