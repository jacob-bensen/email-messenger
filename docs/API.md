# API contract

Canonical contract for the JSON API served by the backend under `/api/v1/**`.
The mobile client (`conexus-mail-mobile`) MUST match the shapes and paths
listed here exactly. Any change to a payload, path, or status code lands in
this file in the same commit as the code change.

The Thymeleaf website (`docs/WEB.md`) is NOT part of this contract — that
surface is server-rendered HTML for browsers and is versioned separately.

## Versioning

- Prefix: `/api/v1`. Breaking changes bump to `/api/v2`.
- Additive changes (new fields on a response, new endpoints) do not bump
  the version; clients ignore unknown fields.
- All responses are `application/json; charset=UTF-8`.
- Timestamps are ISO-8601 UTC strings (e.g. `2026-08-28T14:15:22Z`) unless
  otherwise stated.

## Authentication

- The mobile client authenticates with a bearer token in the
  `Authorization: Bearer <token>` header. **Not yet implemented** — the
  session-cookie login flow at `/login` remains the only auth path today.
  See `INTERNAL_TODO.md` in this repo for the mobile-auth work item.

## Errors

Error responses follow this shape:

```json
{
  "error": {
    "code": "some_slug",
    "message": "Human-readable summary."
  }
}
```

HTTP status codes follow REST conventions (400 validation, 401 unauth,
403 forbidden, 404 not found, 409 conflict, 429 rate-limited, 5xx server).

## Endpoints

### GET /api/v1/health

Unauthenticated. Confirms the API is reachable and returns build info so
the client can detect version drift.

**Response 200**:

```json
{
  "status": "ok",
  "version": "0.1.0-SNAPSHOT",
  "serverTime": "2026-08-28T14:15:22Z"
}
```

Fields:
- `status` (string): always `"ok"` on 200. A stopped or unhealthy backend
  will not return 200 — clients only need to check for HTTP 200 + a
  parseable body.
- `version` (string): the backend's Spring `spring.application.version`,
  which mirrors the Maven `pom.xml` version.
- `serverTime` (string, ISO-8601 UTC): current server clock. Clients may
  compare to their local clock to detect large skew.

## Roadmap

Planned endpoints, in rough priority order. None of these are implemented
yet; the client MUST NOT assume any exist beyond `/api/v1/health`.

- `POST /api/v1/auth/login` — email + password → bearer token.
- `POST /api/v1/auth/logout` — revoke the current bearer token.
- `GET  /api/v1/me` — the authenticated user's profile.
- `GET  /api/v1/chats` — paged list of conversation summaries (mirrors
  the `/chats` Thymeleaf list).
- `GET  /api/v1/chats/{conversationKey}` — one conversation with its
  messages.
- `POST /api/v1/chats/{conversationKey}/messages` — reply.
- `GET  /api/v1/chats/events` — SSE stream of new-message events (mirrors
  the `/chats/events` Thymeleaf endpoint).
