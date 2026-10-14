# API.md — canonical contract

STATUS: **No mobile REST API is implemented.** Everything below §1 is what actually
exists and is reachable programmatically today; §2 is the *planned* `/api/v1`
contract (TBD, not implemented — do not build clients against it until marked live).

## 1. Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | `text/event-stream` of new-mail events (`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe event JSON | 2xx; invalid signature → error (`InvalidStripeSignatureException`) |

Errors for HTML routes go through `GlobalExceptionHandler` → `error.html` (not JSON).

## 2. Planned `/api/v1` (TBD)
Needs, in order: token auth (login → access/refresh token), `GET /api/v1/chats`
(list, filter, search, paging mirroring `/chats`), `GET /api/v1/chats/{key}`
(bubble runs), `POST /api/v1/chats/{key}/send`, `GET /api/v1/mailboxes`,
`POST /api/v1/devices` (push token), live updates (SSE or push). Error envelope:
`{"error": {"code": "...", "message": "..."}}`. All TBD pending the first feature run.
