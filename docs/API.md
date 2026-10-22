# API CONTRACT (canonical)

Status: **no mobile REST API is implemented.** This file documents only the
JSON/non-HTML endpoints that actually exist. Planned mobile endpoints are
listed as TBD and must be specified here *before* implementation.

Conventions today: session-cookie auth, CSRF token required on non-GET
(except where noted), errors rendered via `GlobalExceptionHandler` / `error.html`
(HTML) — no JSON error envelope exists (TBD).

## Existing JSON/machine endpoints

### POST /chats/refresh
- Auth: session. CSRF required.
- Request: no body.
- Response 200 `application/json`: `{ "imported": <int> }` — number of
  newly imported messages after a synchronous poll of the user's mailboxes.
- Errors: 302 to `/login` if unauthenticated; 403 on missing CSRF.

### GET /chats/events
- Auth: session. Response: `text/event-stream` (SSE) of new-mail events
  (`ChatEventStream`, `NewMailEvent`). Event payload schema: TBD (read
  `ChatEventStream` before relying on it).

### POST /billing/webhook
- Auth: none; verified by `Stripe-Signature` header. CSRF-exempt.
- Request: raw Stripe event JSON. Responses: 200 ok; 400 missing/invalid
  signature; 503 webhook not configured; 500 processing failure (Stripe retries).
- Server-to-server only; not for the mobile client.

### GET /actuator/health
- Auth: none. Spring Boot health.

## Planned (TBD — not implemented)
Token auth (login/refresh), conversation list, conversation messages,
send/reply, mailbox list/connect, plan limits. Design in the frontend's
`docs/DESIGN.md` §ROADMAP; each must be added here first.
