# ConexusMail REST API — v1

This document is the **single source of truth** for the REST API exposed by
the `email-messenger` backend for consumption by the ConexusMail mobile
client (`jacob-bensen/conexus-mail-mobile`). Any endpoint added or
changed here MUST land in the same commit as the server implementation and
the client DTOs.

Both the mobile client's networking layer and the Spring controllers under
`com.emailmessenger.api` MUST match this document exactly. If they drift,
the doc is authoritative and the code is the bug.

## Conventions

- **Base path**: `/api/v1`.
- **Transport**: HTTPS in production, plain HTTP OK on localhost.
- **Encoding**: request and response bodies are JSON, UTF-8. Timestamps
  are ISO-8601 with a `Z` suffix (UTC).
- **Errors**: 4xx / 5xx responses carry a JSON body of the shape
  `{ "error": "<slug>", "message": "<human string>" }`. `slug` is
  stable and safe for the client to switch on; `message` is not.
- **Auth**: no endpoint requires auth yet. The first authenticated
  endpoint will introduce a bearer-token scheme; add its shape to this
  file at the same time.
- **Versioning**: breaking changes require a new `/api/v2` prefix; the
  `/api/v1` surface is additive-only after its first release.

## Endpoints

### `GET /api/v1/app-info`

Unauthenticated. Returned on client boot so the mobile app can confirm
connectivity, learn the server's API version, and read whether it is
speaking to a healthy backend before offering to log in.

**Request**: no parameters, no body.

**Response 200**:

```json
{
  "product": "ConexusMail",
  "apiVersion": "v1",
  "serverTime": "2026-08-29T13:45:00Z",
  "status": "ok"
}
```

Field notes:

- `product` — constant string `"ConexusMail"`.
- `apiVersion` — matches the URL prefix; useful once `/api/v2` exists.
- `serverTime` — the server's current UTC time, ISO-8601. Lets the
  client detect a wildly-skewed local clock at boot.
- `status` — `"ok"` when the server is willing to accept requests.
  Reserved values: `"maintenance"` (retryable, brief), `"degraded"`
  (partial). Clients that don't recognize a value should treat it as
  `"degraded"`.

**Response 5xx**: standard error envelope (see Conventions). Client
should treat as "backend unreachable" and offer a retry.

## Deferred / not yet implemented

The client will need these before it can do anything useful. They are
listed as INTERNAL_TODO items in both repos and will be specified here
when implemented; nothing below is contractually implied.

- Auth (bearer-token login exchange for an existing form-login account).
- `GET /api/v1/me` — current-user profile.
- `GET /api/v1/mailboxes` — connected mail accounts for the current user.
- `GET /api/v1/chats` — conversation-list feed (mirrors `/chats` on the web).
- `GET /api/v1/chats/{key}/messages` — messages in one conversation.
- `POST /api/v1/chats/{key}/messages` — reply into a conversation.
- SSE bridge for the existing `/chats/events` new-mail push.
