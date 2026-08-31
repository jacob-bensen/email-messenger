# API.md — REST contract shared with the mobile client

This document is the **single source of truth** for the JSON API that
the `conexus-mail-mobile` client speaks to. Both repos must match it.
Never break this contract without updating this file AND both
implementations in the same commit.

**Base URL (dev):** `http://localhost:8080`
**Base URL (prod):** TBD.
**Path prefix:** `/api/v1/` — every endpoint is versioned. When a
breaking change is unavoidable, bump the version prefix rather than
mutating existing endpoints in place.

**Serialization:** JSON, UTF-8. Timestamps use ISO 8601 in UTC
(`2026-08-31T14:22:31Z`). Field names use `camelCase`. Unknown fields
are ignored on both sides.

**Errors:** `application/json` with the shape below. HTTP status
carries the class of error; the body carries the machine-readable
`code` the client uses to branch on.

```json
{
  "error": {
    "code": "invalid_request",
    "message": "Human-readable reason. Safe to log; not localized."
  }
}
```

Common codes: `invalid_request` (400), `unauthenticated` (401),
`forbidden` (403), `not_found` (404), `conflict` (409),
`rate_limited` (429), `internal` (500).

**Authentication:** TBD. Only `GET /api/v1/status` is defined so far,
and it is public. All authenticated endpoints will use a bearer token
in the `Authorization: Bearer <token>` header once the token flow
lands. Cookie/session auth is reserved for the browser web app. See
`INTERNAL_TODO.md` for the open decision.

---

## Endpoints

### `GET /api/v1/status`

Public. Confirms the API is reachable and reports basic build info so
the mobile app can decide whether it needs to prompt the user to
upgrade.

**Request:** no headers, no body.

**Response 200:**

```json
{
  "service": "email-messenger",
  "apiVersion": "v1",
  "status": "ok",
  "serverTime": "2026-08-31T14:22:31Z"
}
```

Field notes:

- `service` — constant `"email-messenger"`.
- `apiVersion` — constant `"v1"`; changes when the URL prefix does.
- `status` — `"ok"` when the API is serving requests. Any other
  string is a soft signal to the client to surface a status banner.
- `serverTime` — the server's current time in UTC. The client uses it
  only to detect a badly skewed device clock; it is not a source of
  truth for message timestamps.

**Errors:** none in normal operation. A 5xx means infrastructure is
down.

---

## Planned endpoints (not yet implemented)

Recorded here so both repos can plan against them. Do not code
against a planned endpoint — bump it to the "Endpoints" section once
implemented on the server AND the response schema is stable.

- `POST /api/v1/auth/token` — exchange email + password (or a mobile
  OAuth ID token) for a bearer token. Rate-limited.
- `POST /api/v1/auth/token/refresh` — refresh a bearer token.
- `GET /api/v1/me` — the logged-in user (id, email, displayName,
  plan, feature flags). Requires auth.
- `GET /api/v1/chats` — the chats rail: page of conversations for the
  logged-in user, sorted by last activity. Mirrors
  `com.emailmessenger.web.ChatService`.
- `GET /api/v1/chats/{conversationKey}` — one chat's bubble runs.
- `POST /api/v1/chats/{conversationKey}/messages` — send a reply.
  Server-authoritative: SMTP send, thread on the correct
  `In-Reply-To`, record outbound message.
- `GET /api/v1/mailboxes` / `POST /api/v1/mailboxes/imap` — list and
  connect mailboxes (Gmail OAuth flow stays in the browser).
- Live delivery: TBD (WebSocket at `/api/v1/stream`, or long-poll).
  The web app uses SSE; the mobile client needs a decision that
  works on iOS background.
