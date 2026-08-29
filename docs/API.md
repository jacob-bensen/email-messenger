# API Contract

**Source of truth for every REST endpoint the ConexusMail mobile client
(`conexus-mail-mobile`) consumes.** The Thymeleaf website is served from
the same backend but uses form-encoded requests against different paths;
those are documented in `docs/WEB.md`.

## Conventions

- Base prefix: `/api/v1`. Everything below is relative to it.
- Media: `application/json; charset=utf-8`. Requests that carry a body
  MUST set `Content-Type: application/json`.
- Auth: session cookie (`JSESSIONID`) — same Spring Security session the
  website uses. The mobile client obtains it from the login endpoints
  and echoes it in `Cookie:` on subsequent calls.
- CSRF is exempt for `/api/v1/**` in `SecurityConfig` (the cookie is
  same-site-strict and this prefix will only be served to first-party
  clients).
- Errors: JSON `{"error": "<code>", "message": "<human readable>"}`
  with an appropriate HTTP status. `401` = not signed in. `403` = signed
  in but not allowed. `404` = resource missing. `409` = conflict (e.g.
  optimistic reconciliation). `422` = validation error with a `fields`
  map: `{"error": "validation", "fields": {"email": "required"}}`.
- Timestamps: ISO-8601 UTC (`2026-08-29T14:22:31Z`).
- IDs: opaque strings, matched only for equality by the client.

## Endpoints

### GET /api/v1/health

Public liveness ping. The mobile client pings this on launch to gate on a
reachable backend.

- Auth: none.
- Request: no body.
- Response: `200 OK`.

```json
{
  "status": "UP",
  "version": "0.1.0"
}
```

- `status` — always `"UP"` when the process is serving requests.
- `version` — server API contract version (semantic, matches the
  `API_VERSION` constant in `HealthApiController`). Bumped whenever a
  breaking change lands.

The mobile client mirror is `Health` in
`lib/domain/health.dart` (conexus-mail-mobile).

## Planned endpoints (not yet implemented — track in DESIGN.md ROADMAP)

- `POST   /api/v1/auth/session` — email+password login, sets `JSESSIONID`.
- `DELETE /api/v1/auth/session` — logout.
- `GET    /api/v1/me` — current user summary.
- `GET    /api/v1/conversations?cursor=…` — paged chats list.
- `GET    /api/v1/conversations/{key}/messages?cursor=…` — one thread.
- `POST   /api/v1/conversations/{key}/messages` — reply.
- `GET    /api/v1/chats/events` (SSE) — live updates.
- `GET    /api/v1/mailboxes` / `POST /api/v1/mailboxes` — connect IMAP.
- `POST   /api/v1/devices` — register push token (FCM/APNs).

Each of these MUST be defined here BEFORE the backend implements it and
BEFORE the mobile client mirrors it. If a change ever breaks the shape,
both repos ship the change in the same commit.
