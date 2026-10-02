# API.md — canonical API contract

**Status (2026-10-02): no mobile REST API exists.** This file records what is actually exposed today and reserves
the design space. Per the sync routine, the contract must be written here BEFORE implementing any endpoint; the
mobile client must match it exactly. Server is authoritative.

## Conventions (proposed, TBD until first endpoint ships)
- Base path `/api/v1`, JSON, UTF-8. Errors: `{ "error": "<code>", "message": "<text>" }` with 400/401/403/404/409/422.
- Auth: **TBD** — requires a token scheme (Bearer) because current auth is session cookie + CSRF.

## Endpoints that exist today (not mobile-grade)
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | — | `200 {"imported": <int>}` |
| `GET /chats/events` | session | — | `text/event-stream`; event `new-mail`, data `{"imported": <int>}` |
| `POST /billing/webhook` | Stripe signature (`Stripe-Signature`), CSRF-exempt | raw Stripe event JSON | `200 "ok"`; `400` missing/invalid signature; `503` not configured; `500` apply failure (Stripe retries) |

All other routes return HTML (see `WEB.md`).

## Planned (not implemented; proposals only)
Auth token issue/refresh; `GET /api/v1/conversations` (list, filter, mailbox scope, paging);
`GET /api/v1/conversations/{key}`; `POST /api/v1/conversations/{key}/messages`; mailbox list; plan/limits.
