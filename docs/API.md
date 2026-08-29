# API contract (mobile REST surface)

**Canonical.** This document is the single source of truth for every REST
endpoint the backend exposes to the `conexus-mail-mobile` Flutter client.
Both repositories MUST match it exactly. Do not implement a route on one
side without updating this file in the same commit.

Scope: `/api/**` only. The Thymeleaf website's routes (`/`, `/chats/**`,
`/mailboxes/**`, `/account/**`, `/login`, `/register`, …) are NOT part of
this contract — see `docs/WEB.md` for those.

## Conventions

- **Base URL.** `/api/v1` is the current major version. Additive changes
  (new endpoints, new optional fields) stay under `v1`; breaking changes
  cut a new `v2`.
- **Wire format.** JSON only, UTF-8. `Content-Type: application/json` on
  every request body and response body.
- **Casing.** Request and response JSON keys are `camelCase`. Timestamps
  are RFC 3339 / ISO 8601 UTC strings ending in `Z` (e.g.
  `2026-08-29T14:03:07Z`).
- **Auth.** TBD — see `INTERNAL_TODO.md`. For v1 milestone 0 only the
  health endpoint is public; every subsequent endpoint will require a
  bearer token in the `Authorization: Bearer <token>` header. CSRF is
  disabled for `/api/**`.
- **Errors.** All non-2xx responses use this shape:
  ```json
  {
    "error": {
      "code": "SNAKE_CASE_SLUG",
      "message": "Human-readable description.",
      "details": { "…": "…" }   // optional, endpoint-specific
    }
  }
  ```
  Reserved codes: `unauthorized`, `forbidden`, `not_found`,
  `validation_failed`, `rate_limited`, `internal_error`.
- **Rate limiting.** Not yet enforced on `/api/**`. When added, responses
  include `X-RateLimit-Remaining` and `Retry-After` on 429.

## Endpoints

### `GET /api/v1/health`

Liveness probe for the mobile client. No auth. Safe to call on every app
launch and from the "About" screen to confirm connectivity.

**Request.** No body, no query parameters, no auth.

**Response 200.**
```json
{
  "status": "ok",
  "version": "0.1.0-SNAPSHOT",
  "time": "2026-08-29T14:03:07Z"
}
```

Fields:
- `status` — always the literal string `"ok"` on a 200. Any degraded
  state returns 503 with the standard error envelope.
- `version` — the server build's `spring-boot-maven-plugin` artifact
  version (from `pom.xml`). Purely informational for the client.
- `time` — server wall-clock at response time, in UTC. Clients MAY use
  it to detect large local-clock skews but MUST NOT rely on it for
  auth or ordering.

**Response 503.** Standard error envelope, `code: "unavailable"`.

## Roadmap (not yet implemented)

Ordered rough-priority. Each item requires adding both server and client
code in the same run.

- Bearer/JWT auth: `POST /api/v1/auth/login`,
  `POST /api/v1/auth/register`, `POST /api/v1/auth/logout`,
  `POST /api/v1/auth/refresh`.
- Session profile: `GET /api/v1/me`.
- Chats list: `GET /api/v1/chats` (paginated by
  `conversationKey`-ordered `updatedAt`).
- Chat detail: `GET /api/v1/chats/{conversationKey}`.
- Send reply: `POST /api/v1/chats/{conversationKey}/messages`.
- Mailbox list / connect / delete: `GET /api/v1/mailboxes`,
  `POST /api/v1/mailboxes`, `DELETE /api/v1/mailboxes/{id}`.
- Live stream: SSE at `/api/v1/events` (mirrors what `/chats/events`
  does for the web).

Everything on this roadmap MUST land in this file first, then be
implemented on the backend, then on the mobile client — in that order.
