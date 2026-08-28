# API contract — ConexusMail

Canonical contract between the backend (this repo) and the mobile
client (`jacob-bensen/conexus-mail-mobile`). **Both sides depend on
this file.** Any change ships in one PR that updates the contract, the
server implementation, and the client implementation together.

## Status

**No public REST API exists yet.** Every controller in
`com.emailmessenger.web`, `.auth`, `.admin`, `.billing`, `.team`, and
`.digest` returns HTML for the Thymeleaf website. Session auth is a
`JSESSIONID` cookie (see `docs/BACKEND.md`), which is browser-only —
not usable from a Flutter client.

The first mobile-facing feature to build is therefore a small REST
surface under `/api/v1/*` with a token-based auth model. Until then
this document is a placeholder; endpoints appear here as they land.

## Conventions (target — enforce as endpoints are added)

- **Base path**: `/api/v1`. Every mobile endpoint lives under it.
- **Content type**: JSON in, JSON out. `Content-Type: application/json`
  is required on requests with a body. Responses are always UTF-8 JSON,
  including errors.
- **Auth**: `Authorization: Bearer <token>` for authenticated
  endpoints. Tokens are issued by the login endpoint. See the auth
  section below when it is implemented.
- **Time**: ISO-8601 UTC with `Z` suffix (e.g. `2026-08-28T14:03:11Z`).
  No local offsets on the wire.
- **IDs**: server-issued opaque strings. The client never invents them.
- **Errors**: `{ "error": { "code": "snake_case_code", "message": "human readable" } }`
  with an appropriate HTTP status. Never leak stack traces.
- **Pagination**: cursor-based. Responses return
  `{ "items": [...], "next_cursor": "..." | null }`. Clients pass
  `?cursor=...&limit=N` (server clamps `limit`).
- **CSRF**: not applicable — REST endpoints are stateless and
  token-authenticated, so the server-side CSRF filter must exclude
  `/api/v1/**` when the API ships.
- **Versioning**: additive changes are unversioned; breaking changes
  bump to `/api/v2` and both versions run until the mobile floor moves.

## Endpoints

_None yet._ New endpoints go here in this shape:

```
### <VERB> <path>
- Auth: none | bearer
- Request: { ... schema ... }
- Response 200: { ... schema ... }
- Errors: 400 invalid_argument, 401 unauthenticated, ...
- Notes: any invariants, rate limits, side effects
```

## DTO reference

_None yet._ Shared DTO shapes (message, chat, participant,
attachment-ref) will be documented here as endpoints add them, so the
Flutter `lib/domain/` and the Java records can mirror them without
guessing.

## Not exposed to the mobile client

The following server endpoints exist but are **HTML-only** and are not
part of the mobile contract. Do not call them from the client — they
require a `JSESSIONID` and return HTML:

- All of `/`, `/login`, `/register`, `/chats`, `/chats/*`, `/mailboxes`,
  `/mailboxes/*`, `/account/*`, `/team/*`, `/admin/*`, `/billing/*`,
  `/searches`, `/pricing`, `/demo`, `/privacy`, `/terms`, `/refund`,
  `/digest/opt-out`.
- PWA assets: `/manifest.webmanifest`, `/sw.js`, `/offline`,
  `/icons/*`, `/apple-touch-icon.png`.
- SEO: `/robots.txt`, `/sitemap.xml`.
- SSE stream: `GET /chats/events` — browser-only for now. The mobile
  equivalent will be a separate `/api/v1/events` endpoint (SSE or
  WebSocket, TBD) that authenticates by bearer token.

These are catalogued in `docs/WEB.md`.
