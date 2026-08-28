# API.md — canonical REST contract

This file is the single source of truth for the JSON REST API served by the
ConexusMail backend and consumed by the ConexusMail mobile client. Both
implementations MUST match this contract exactly. Changes to any endpoint,
request body, response body, or error code MUST update this file first, then
both implementations, in the same run.

## Conventions

- **Base path:** `/api/v1`.
- **Transport:** HTTPS in production; HTTP allowed for `localhost` dev only.
- **Content type:** JSON in and out. Bodies are UTF-8 encoded.
- **Field naming:** `camelCase`.
- **Timestamps:** ISO-8601 with UTC `Z` suffix (e.g. `2026-08-28T12:34:56Z`).
- **Auth:** none for the endpoints listed below. Bearer-token flow will be
  defined here when the first authenticated endpoint lands.
- **Errors:** unless otherwise stated, non-2xx responses use the shape
  ```json
  { "error": "SHORT_CODE", "message": "human readable" }
  ```
  and the status code is chosen per RFC 9110.

## Endpoints

### `GET /api/v1/health`

Liveness + version probe for the mobile client. Cheaper and more stable than
`/actuator/health` because the shape here is owned by us. Safe to call
unauthenticated.

**Auth:** none.

**Request:** no body.

**200 OK response:**

```json
{
  "status": "ok",
  "version": "0.1.0-SNAPSHOT",
  "serverTimeUtc": "2026-08-28T12:34:56Z"
}
```

| Field           | Type   | Notes                                                    |
| --------------- | ------ | -------------------------------------------------------- |
| `status`        | string | Always the literal `"ok"` while the server is serving.   |
| `version`       | string | Maven artifact version of the running backend build.     |
| `serverTimeUtc` | string | ISO-8601 UTC timestamp captured when the request arrived. |

**Error responses:** none. If the server can't respond, the request fails at
the transport layer.

## Change log for the contract

- **2026-08-28** — Contract file created. Added `GET /api/v1/health` (initial
  end-to-end vertical for the mobile client).
