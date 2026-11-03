# API — canonical contract (mobile client ⇄ backend)

Status as of 2026-11-03: **no mobile REST API is implemented.** The server
exposes only the browser-oriented routes in `docs/WEB.md` (session cookie +
CSRF, HTML responses). The Flutter client (`conexus-mail-mobile`) is an empty
scaffold and calls nothing.

This file is the contract that both repos must follow. Add endpoints here
**before** implementing them. Nothing below `Planned` is real.

## Existing machine-readable endpoints (implemented, browser-oriented)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | SSE stream; event `new-mail`, data `{"imported": <int>}`; emitter timeout 30 min |
| POST | `/billing/webhook` | Stripe signature | Stripe event JSON | 2xx / error (CSRF-exempt) |
| GET | `/actuator/health` | public | — | Spring health |

Errors: `GlobalExceptionHandler` renders HTML error pages; no JSON error
envelope exists yet.

## Planned (TBD — not implemented)

Proposed conventions for `/api/v1/**`: JSON, stateless bearer-token auth
(login → access + refresh token), JSON error envelope
`{"error": {"code": "...", "message": "..."}}`, no CSRF. First slice
(see `INTERNAL_TODO.md`): `POST /api/v1/auth/login`, `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`. Schemas to be
defined here when that feature is picked up.
