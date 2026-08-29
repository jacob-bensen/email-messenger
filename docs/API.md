# ConexusMail API

**Base URL (local dev):** `http://localhost:8080`
**API prefix:** `/api/v1`
**Content type:** `application/json` (UTF-8)
**Authoritative source:** this file. The Flutter client
(`conexus-mail-mobile`) and the Spring Boot backend (`email-messenger`) MUST
match it exactly. When you change the contract, update this file in the
same commit as the code on both sides.

## Versioning

- Path-prefixed: `/api/v1/...`. Breaking changes bump the prefix
  (`/api/v2`); additive changes stay in `v1`.
- Every response carries `apiVersion` where useful for the client to
  double-check.
- The unversioned Thymeleaf paths (`/chats`, `/mailboxes`, `/login`, ...)
  are the website surface, not the mobile API. They are NOT stable for
  programmatic use — treat them as internal.

## Authentication (current state)

The backend today only supports session-cookie + Spring Security form login
(plus Google OIDC) — used by the website. The mobile client cannot yet
authenticate to the API. `INTERNAL_TODO.md` tracks the mobile-auth work
(OAuth2 password grant or personal-access-token style). Until that lands,
only endpoints marked `Auth: public` are usable from the app.

## Error shape

Non-2xx responses follow the platform default (Spring's `problem+json`
where already wired in, or a plain JSON body with an `error` key). This
section will formalise once the mobile client encounters the first real
error path — see `INTERNAL_TODO.md`.

---

## Endpoints

### `GET /api/v1/health`

Liveness probe the mobile app hits on launch to confirm reachability.
Distinct from `/actuator/health` (that stays for platform probes and
Spring's own health indicators).

- **Auth:** public
- **Request:** none
- **Response 200:**

  ```json
  {
    "status": "ok",
    "apiVersion": "v1",
    "serverTime": "2026-08-29T18:00:00Z"
  }
  ```

  | Field        | Type   | Notes                                              |
  |--------------|--------|----------------------------------------------------|
  | `status`     | string | Always `"ok"` when the server responds.            |
  | `apiVersion` | string | Currently `"v1"`.                                  |
  | `serverTime` | string | Server clock, ISO-8601 UTC (`Instant.toString()`). |

- **Errors:** none defined; a network failure or 5xx means "server
  unreachable", surfaced in the client as an offline banner.

---

## Planned (see `INTERNAL_TODO.md`)

- Mobile auth (token exchange, refresh, revoke).
- `GET /api/v1/chats` — paged conversation list keyed by
  `conversationKey`, mirrors the web `/chats` list.
- `GET /api/v1/chats/{key}` — one conversation's merged timeline.
- `POST /api/v1/chats/{key}/send` — send a message (reply or new).
- `GET /api/v1/mailboxes` — connected mail accounts.
- Server-Sent Events or WebSocket for live "new mail" push (the web app
  already uses SSE at `/chats/events`; the mobile app will get its own
  channel that carries user id in a header, not a session cookie).
