# API contract (canonical)

## Status: NO MOBILE REST API EXISTS YET

As of 2026-10-14 the backend exposes only server-rendered HTML routes (see `docs/WEB.md`),
the Stripe webhook (`POST /billing/webhook`, raw JSON + signature, not for clients), and an
SSE stream (`GET /chats/events`, session-authenticated). The mobile client has no contract to
code against. Sections below are TBD and are the planned first slice, to be defined
before implementation.

## Planned conventions (TBD, not implemented)
- Base path `/api/v1`, JSON, UTF-8.
- Auth: TBD. Session+CSRF is unsuitable for mobile; proposal is bearer access/refresh tokens
  issued by `POST /api/v1/auth/login`, reusing `DbUserDetailsService`/`LoginThrottleService`.
- Error body: `{ "error": "<code>", "message": "<text>" }` (TBD).

## Endpoints
None implemented. First candidates: auth login/refresh/logout, `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send` — each backed by the service layer
behind `ConversationController`.
