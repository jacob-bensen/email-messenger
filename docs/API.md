# API contract (mobile client)

**Status: not yet defined.** There is currently no REST API for the
`conexus-mail-mobile` client. Every backend controller in this repo is
either a Thymeleaf page controller, a Stripe webhook, a static/PWA asset
endpoint, or the SSE stream at `/chats/events`.

This file is the canonical contract the mobile client must match once
the API exists. Do not add mobile-facing endpoints on the server or
network calls in the client without updating this file first.

## Conventions (once endpoints are added)

- Base path: `/api/v1`
- Authentication: session cookie for now (same session the web uses);
  bearer tokens if/when the mobile client can't share a browser session.
- Request/response bodies: JSON.
- Timestamps: ISO-8601 UTC (`2026-08-31T12:34:56Z`).
- Errors: `{"error": "<code>", "message": "<human-readable>"}` with an
  appropriate HTTP status.
- Pagination: cursor-based; server returns `nextCursor` when more.
- All state mutations run in a single server transaction (read →
  validate → mutate → audit). The server never trusts client-supplied
  values it can recompute.

## Endpoints

_None defined yet._ When the first endpoint lands, document it here as:

```
### POST /api/v1/<path>
Auth: session
Body: { … }
Response: 200 { … }
Errors: 400 invalid_input, 401 unauthenticated, 403 forbidden, 404 not_found
```

## Notes for future work

The Thymeleaf controllers already have URL patterns (`/chats`,
`/chats/{key}`, `/chats/{key}/send`, `/chats/{key}/pane`,
`/chats/events`, `/mailboxes/**`, `/account/**`, `/billing/**`,
`/team/**`) that a mobile-facing API should mirror in shape and
delegate to the same service classes. See `WEB.md` for the full page
inventory and the services each one uses.
