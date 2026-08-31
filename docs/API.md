# ConexusMail API contract (v1)

This is the CANONICAL contract between the backend (`email-messenger`) and
every non-browser client (currently `conexus-mail-mobile`, a Flutter app).
Both repos MUST match this document exactly. Any change to a request or
response shape lands here first, in the same commit as the server change,
and the mobile client is updated in the same run.

The Thymeleaf website is not a client of this API — it uses server-side
Spring services directly. Contract drift between the two is prevented by
sharing the service layer, not by calling the API from the browser.

## Base URL and versioning

```
https://<host>/api/v1
```

Every endpoint under `/api/v1/**` is versioned. A breaking change ships as
`/api/v2/**` alongside; `/api/v1/**` keeps working until the mobile app's
minimum supported build no longer needs it.

## Transport

- JSON in and JSON out (`Content-Type: application/json;charset=UTF-8`).
- Timestamps are RFC 3339 in UTC (`"2026-08-31T12:34:56Z"`), serialized by
  Jackson from `java.time.Instant`.
- Enums are UPPER_SNAKE_CASE strings.
- Money is minor units (cents) as a JSON integer with a sibling ISO-4217
  `currency` field.

## Statelessness

`/api/**` is **stateless**. No `JSESSIONID` cookie is required or issued.
CSRF is disabled for `/api/**` in `SecurityConfig` — mobile clients carry
their own auth token (see Auth below) and never rely on a session.

The Thymeleaf website's `/chats/events` SSE stream and `/chats/refresh`
POST are separate — they use the browser session and CSRF, and are NOT
part of this contract. Do not call them from the mobile client.

## Auth

- **v1 (current):** `GET /api/v1/health` is unauthenticated. No other API
  endpoints exist yet.
- **v1 (planned):** every other endpoint will require a bearer token in
  the `Authorization: Bearer <token>` header. Token issuance
  (`POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`) is the next
  planned change — see `INTERNAL_TODO.md`.

Errors from auth: `401 Unauthorized` (missing/expired token),
`403 Forbidden` (token valid, resource off-limits).

## Error shape

Non-2xx responses use this JSON body:

```json
{
  "error": {
    "code": "PLAN_LIMIT_REACHED",
    "message": "Free plan is limited to 3 mailboxes."
  }
}
```

- `error.code`: a stable UPPER_SNAKE_CASE identifier. Clients switch on it.
- `error.message`: human-readable, safe to show. English only for now.

All errors on `/api/**` go through `api/ApiExceptionHandler`, which is
scoped to the `api` package. It returns JSON — never the HTML error page
that the website's `web/GlobalExceptionHandler` renders.

`error.code` values currently emitted:

- `METHOD_NOT_ALLOWED` (405) — the URL exists but not for that HTTP
  method. `Allow` header lists the supported methods.
- `BAD_REQUEST` (400) — the request was malformed (e.g. invalid JSON,
  missing required field).
- `INTERNAL_ERROR` (500) — unexpected server error. The client should
  retry with backoff.

## Endpoints

### `GET /api/v1/health`

Liveness probe callable by mobile clients on cold start (and by uptime
monitors). No auth. Never mutates state.

**Response 200:**

```json
{
  "status": "ok",
  "apiVersion": "v1",
  "serverTime": "2026-08-31T12:34:56.789Z"
}
```

- `status`: always `"ok"` when the process is up. The server never returns
  a non-2xx for this endpoint; use `/actuator/health` for deep readiness.
- `apiVersion`: the API version this server implements. Must match the
  version the client was built against; a mismatch triggers a client-side
  "please update" screen (see `docs/MOBILE.md`).
- `serverTime`: the server's current UTC time. The client uses this to
  detect device clock skew.

Backing controller: `api/ApiHealthController.java`.

## Not yet implemented (see INTERNAL_TODO.md)

Everything else the mobile client needs — auth tokens, chats list,
conversation fetch, send, mailbox list, live-mail stream over WebSocket
or SSE — is still TBD. Each will be defined in this file BEFORE the
implementation lands, per the sync routine's contract-first rule.
