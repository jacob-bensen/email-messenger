# API.md — canonical API contract (2026-11-09)

**Status: no mobile-facing REST API exists.** The server exposes only the endpoints below that return non-HTML.
Everything else is browser/session/CSRF-based Thymeleaf (see `docs/WEB.md`). The mobile repo
(`conexus-mail-mobile`) is an empty scaffold, so there is no client to be out of sync with yet.

Rule: when `/api/v1` is added, document each endpoint here FIRST, then implement on both sides.

## Implemented non-HTML endpoints
### `POST /chats/refresh` — auth: session + CSRF
Synchronously polls the user's active mailboxes. 200 `{"imported": <int>}`. Unauthenticated: 302 to `/login`.

### `GET /chats/events` — auth: session
`text/event-stream`. Event name `new-mail` (payload: see `ChatEventStream`, TBD/undocumented shape).
Emitter timeout 30 min; client must reconnect.

### `POST /billing/webhook` — auth: Stripe signature, CSRF exempt
Header `Stripe-Signature` required. 200 ok; 400 `missing Stripe-Signature header` / `invalid signature`; 503 `webhook not configured`.

### `GET /actuator/health` — public.

## Planned (NOT implemented — TBD)
Token auth (login → access/refresh token), `GET /api/v1/chats`, `GET /api/v1/chats/{key}`,
`POST /api/v1/chats/{key}/messages`, mailbox list, push registration, error envelope.
Shapes are deliberately not defined until the first feature run designs them.
