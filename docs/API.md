# ConexusMail REST API contract

This is the **single source of truth** for HTTP endpoints consumed by the
`conexus-mail-mobile` Flutter client. Both the server implementation and the
Dart networking layer are derived from this file. Any change here MUST land
in the same run as the code that implements or consumes it.

## Base URL and versioning

- Base path: `/api/v1`
- All request/response bodies are JSON (`Content-Type: application/json`).
- Backward-incompatible changes bump the path prefix (`/api/v2`).
- Timestamps are ISO-8601 UTC strings.

## Auth

**Session-cookie auth is the current mechanism** — mobile clients POST to
`/login` with `email` + `password` to obtain a `JSESSIONID` cookie, then
present it on subsequent requests. This mirrors the website's login exactly
so both surfaces use one auth path.

A dedicated mobile auth flow (bearer tokens with refresh, mTLS, or
device-scoped API keys) is TBD — tracked in `INTERNAL_TODO.md`.

Endpoints marked **PUBLIC** require no session.

## Error format

Non-2xx responses use RFC 7807-style JSON:

```json
{
  "status": 400,
  "code": "validation_failed",
  "message": "human-readable description",
  "fields": { "email": "must be a valid address" }
}
```

`code` is a machine-readable slug the client can switch on. `fields` is
optional and only present for validation errors.

## Endpoints

### GET /api/v1/health — **PUBLIC**

Liveness + basic version probe. Returns 200 whenever the app is up.

Response 200:

```json
{
  "status": "ok",
  "service": "conexusmail-backend",
  "version": "0.1.0-SNAPSHOT",
  "time": "2026-08-30T12:34:56Z"
}
```

- `status` — always `"ok"` from this endpoint. Deeper health with dependency
  checks lives at Spring Actuator's `/actuator/health` (not part of the
  mobile contract).
- `service` — stable identifier.
- `version` — the Maven artifact version.
- `time` — server time at response.

The mobile client uses this at startup to confirm reachability and (later)
to compare `version` against its minimum supported server.

## Contract change process

1. Edit this file first.
2. Implement server-side (Spring controller + service).
3. Update `lib/data/api_client.dart` and the matching DTO in
   `conexus-mail-mobile`.
4. Commit both repos in the same run; note the contract change in each
   CHANGELOG.md.
