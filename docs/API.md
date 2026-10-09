# API contract

**Status (2026-10-09): there is no REST API yet.** The backend serves only
server-rendered HTML for browsers. This file is the canonical contract; it
records (A) what exists and is usable programmatically today and (B) what is
*proposed* for the mobile client. Section B is TBD and **not implemented** —
neither repo may rely on it until it moves into section A.

## A. Existing non-HTML endpoints
All require the browser session cookie + CSRF token unless noted.

| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — messages newly imported for the caller's due mailboxes (synchronous) |
| `GET /chats/events` | session | none | `text/event-stream`; signals new mail (see `ChatEventStream`, `NewMailEvent`). Event payload schema: TBD |
| `POST /billing/webhook` | none; Stripe signature header | raw Stripe JSON | `200` on success; invalid signature → error (`InvalidStripeSignatureException`). Not for clients |
| `GET /actuator/health` | none | — | Spring health JSON |

Errors: unauthenticated requests are redirected (302) to `/login`, not 401 —
unsuitable for API clients. Error JSON shape: TBD (none defined).

## B. Proposed (unimplemented) mobile API — TBD
Needed before a client can exist: token auth (`POST /api/v1/auth/login`),
`GET /api/v1/chats`, `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/messages`,
`GET /api/v1/mailboxes`, device registration for push. Schemas to be defined
when the first feature is chosen (contract first).
