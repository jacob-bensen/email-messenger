# API contract

**Status: no mobile REST API exists yet.** This file documents what is actually implemented and, under "Planned", the intended `/api/v1` surface (TBD — not implemented; clients must not call it).
All current endpoints use the browser session cookie + CSRF token; none accepts bearer tokens.

## Implemented non-HTML endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous mailbox refresh for the user |
| GET | `/chats/events` | session | none | `text/event-stream`; `NewMailEvent` payloads (see `ChatEventsController`; payload schema TBD) |
| POST | `/billing/webhook` | Stripe signature header | Stripe event JSON | 2xx on handled; invalid signature → rejected (`InvalidStripeSignatureException`) |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/robots.txt`, `/sitemap.xml` | public | — | PWA/SEO assets |

Errors: HTML endpoints render `error.html` via `GlobalExceptionHandler`; JSON error schema TBD.

## Planned (TBD, not implemented)
- Token auth (login → access/refresh token).
- `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`, mailbox list/connect, push-subscription registration.
These must be added here first, then implemented in `ChatService`-backed controllers, then in the client.
