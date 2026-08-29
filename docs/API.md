# API — canonical contract

This file is the **single source of truth** for the network contract
between the backend (`email-messenger`) and any non-browser client
(today: `conexus-mail-mobile`). Both repos MUST match this file exactly.

## Status: no REST API yet

As of 2026-08-29 the backend exposes no bearer-token-authenticated REST
endpoints. Every `*Controller` is a Thymeleaf `@Controller` returning a
view name, authenticated by a browser session cookie. The only JSON
endpoint is `POST /billing/webhook` (Stripe webhook — server-to-server,
signature-verified, not a general API).

Adding a mobile client (see `../conexus-mail-mobile/docs/DESIGN.md`)
requires designing and building a REST or GraphQL surface, and picking a
token-based auth model (session cookies won't survive a mobile app).
That is a strategic decision for a human; it is tracked as the first
item in `INTERNAL_TODO.md` in both repos.

## Contract conventions (when the API is designed)

When the first non-browser endpoint lands, follow these conventions so
both repos can grow in lockstep:

- **Base path:** `/api/v{n}` (start at `/api/v1`). Never overlap with an
  existing Thymeleaf route.
- **Auth:** decide once (OAuth2 password grant, Google-only OIDC with a
  first-party token exchange, or long-lived personal tokens). Whichever
  is picked, add it to `docs/BACKEND.md` §"Auth model" and to
  `docs/MOBILE.md` §"Networking" in the same commit.
- **Media type:** `application/json`, UTF-8. Timestamps as RFC 3339
  strings in UTC (`2026-08-29T13:45:00Z`), never epoch millis.
- **Errors:** RFC 7807 `application/problem+json` — `{ "type", "title",
  "status", "detail", "instance" }`. Non-2xx MUST include this shape.
- **IDs:** stringified Long (`"42"`) — mobile clients get bitten by
  53-bit JSON numeric truncation and this is cheaper than fighting it
  later. The backend also uses `Long` PKs today.
- **Pagination:** cursor-based, `?cursor=<opaque>&limit=<int>`. Responses
  return `{ "items": [...], "next_cursor": "…" | null }`.
- **Versioning:** never rename a field in place; add the new one, ship
  both for one release, then remove the old — and update this file in
  the same commit as either half of the change.
- **Server authority:** the mobile client MUST NOT compute anything the
  server can compute (plan limits, message ordering, thread grouping,
  billing state, unread counts). Optimistic prediction is fine but the
  server value always wins on reconcile.

## Endpoint inventory

_None yet._ When endpoints are added, list them here in this shape:

```
### GET /api/v1/example
- Auth: bearer
- Query: none
- 200 response:
  { "id": "42", "name": "…", "created_at": "2026-08-29T00:00:00Z" }
- Errors: 401 (missing/invalid token), 404 (not found)
```

Do not implement any endpoint whose entry is not first in this file.
