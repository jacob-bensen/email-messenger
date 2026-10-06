# API.md — canonical API contract (2026-10-06)

**Status: no mobile REST API exists.** The backend is server-rendered; there is no
`/api/**`, no bearer/token auth, and no JSON DTOs shared with a client. The mobile repo must
not call anything until the contract below is extended and implemented in the same run.

## Existing non-HTML endpoints (session cookie auth, CSRF token required for POST)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session | `200 {"imported": <int>}` — newly imported message count |
| GET | `/chats/events` | session | `text/event-stream` (SSE); `NewMailEvent` per new mail |
| POST | `/billing/webhook` | Stripe signature header; no CSRF | Stripe event receiver; invalid signature → error via `InvalidStripeSignatureException` |

Error codes: TBD (errors currently render `templates/error.html` via `GlobalExceptionHandler`).

## Planned (TBD — not implemented)
`/api/v1` with token auth (login → access/refresh token), `GET /conversations`,
`GET /conversations/{key}`, `POST /conversations/{key}/messages`, mailbox list/connect, plan
limits. Shapes to be specified here first, backed by the existing services (see `BACKEND.md`).
