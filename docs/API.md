# API.md — canonical API contract

Status 2026-10-29: **no mobile-oriented REST API exists.** The server is session+CSRF, HTML-first. This file records
(1) the JSON/stream endpoints that exist today, and (2) the `/api/v1` target the mobile client will be built against.
Nothing in section 2 is implemented; do not code a client against it until it is moved to section 1.

## 1. Implemented non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous poll of user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; event `new-mail`, data `{"imported": <int>}` |
| POST | `/billing/webhook` | Stripe signature header, CSRF exempt | Stripe event JSON | 200 / 400 on bad signature |
| GET | `/actuator/health` | public | | Spring health JSON |
Errors: unauthenticated → 302 to `/login`; CSRF failure → 403 (`GlobalExceptionHandler` / Spring Security).

## 2. Planned `/api/v1` (TBD, not implemented)
Auth: token (e.g. `POST /api/v1/auth/login` → access+refresh token) — design TBD; CSRF not applicable to Bearer calls.
Candidate first slice (drives the first feature run): `POST /api/v1/auth/login`, `GET /api/v1/chats?filter=&q=&page=`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`, `GET /api/v1/chats/events`. Error body shape:
`{"error": "<code>", "message": "<text>"}` (proposed). Each must delegate to the existing services.
