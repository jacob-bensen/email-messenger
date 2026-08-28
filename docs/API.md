# API

Canonical REST API contract for the ConexusMail mobile client. Both the
backend (`../email-messenger`) and the mobile client
(`../conexus-mail-mobile`) MUST match this document exactly. Change this
file *before* changing either implementation.

Base path: **`/api/v1`**. All responses are JSON encoded as UTF-8.
Timestamps are ISO-8601 with UTC offset (`"2026-08-28T13:45:00Z"`).

## Conventions

- Successful responses use `200 OK` with the documented body.
- Client errors use `400 / 401 / 403 / 404 / 409 / 422` with an error
  envelope:
  ```json
  { "error": { "code": "STRING_ENUM", "message": "human readable" } }
  ```
- Server errors use `500` with the same envelope and `code: "internal"`.
- The API never returns HTML redirects. Web-only session-restore flows
  are Thymeleaf territory and live under other paths.

## Authentication

The current cut of the API exposes only public endpoints. Auth-bearing
endpoints will follow in later runs and will use bearer tokens issued by
`POST /api/v1/session` (planned; not yet implemented). Tracked in
`INTERNAL_TODO.md`.

## Endpoints

### `GET /api/v1/health`

Liveness probe suitable for the mobile app's cold-start connectivity
check.

**Auth:** none.

**Response 200:**
```json
{
  "status": "ok",
  "timestamp": "2026-08-28T13:45:00Z"
}
```

- `status` — always the literal string `"ok"` when the server responds.
  Any other status means the API is functioning at the HTTP layer but
  something downstream is degraded; the client should treat it as
  informational and not fail hard.
- `timestamp` — server wall-clock at the moment the response was built.

### `GET /api/v1/version`

Reports the running backend build so the mobile client can display it
in its About screen and warn on incompatible major versions later.

**Auth:** none.

**Response 200:**
```json
{
  "name": "email-messenger",
  "version": "0.1.0-SNAPSHOT",
  "api": "v1"
}
```

- `name` — the Spring Boot `spring.application.name`, currently
  `email-messenger`.
- `version` — the Maven `${project.version}`, resolved at build time
  from `pom.xml`. `SNAPSHOT` suffix in dev, plain semver in release
  builds.
- `api` — the API namespace this response was served from. Kept in the
  body so a client already speaking `v2` can detect a `v1` fallback if
  it accidentally hits an old server.

## Not yet in the contract (tracked)

- `POST /api/v1/session` — email/password + Google-idtoken exchange for
  a bearer token.
- `DELETE /api/v1/session` — revoke the current token.
- `GET /api/v1/me` — current user summary (plan, mailbox count).
- `GET /api/v1/chats` — conversation list (participant-set keyed
  bubbles, unread counts).
- `GET /api/v1/chats/{conversationKey}` — full conversation with
  bubble-run projection.
- `POST /api/v1/chats/{conversationKey}/reply` — outbound reply.
- `GET /api/v1/mailboxes` and connect flows for IMAP + Gmail OAuth.

Each addition lands here first, then in both implementations.
