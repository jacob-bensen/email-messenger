# API.md — REST contract for the mobile client

This file is the **single source of truth** for the JSON API served by
`email-messenger` and consumed by `conexus-mail-mobile`. Any change to
an endpoint must land in this file in the SAME commit as the code
change on either side.

Rules:

- All endpoints under `/api/v1/**` unless stated otherwise.
- All request and response bodies are `application/json; charset=utf-8`.
- Timestamps are ISO-8601 in UTC, e.g. `2026-08-31T14:20:00Z`.
- IDs are integers unless the field name says otherwise
  (`conversationKey` is an opaque string).
- Errors follow a single envelope: `{"error": {"code": "SLUG",
  "message": "human readable"}}` with an appropriate 4xx/5xx status.
- Auth on `/api/v1/**` is bearer-token — see `POST /api/v1/auth/login`.
  Session cookies are NOT accepted on `/api/**` (the mobile client
  does not manage cookies).
- The server is authoritative. The client MUST accept the server's
  value for any conflicted field — server wins on reconcile.

## Legend

- ✅ Implemented
- 🚧 Planned — in this doc but not in code yet
- ⛔ Deprecated — clients must migrate off

## Endpoints

### Health

- ✅ `GET /actuator/health` → Spring Boot Actuator's stock probe.
  Used by uptime monitoring. Not part of the mobile contract.

### `/chats/**` (website; NOT the mobile contract)

The `/chats/**` and `/chats/events` routes are the Thymeleaf
workspace. Only `POST /chats/refresh` currently emits JSON:

- ✅ `POST /chats/refresh` → `{"imported": integer}`
  - Auth: session cookie (website only).
  - Semantics: synchronously polls the caller's active mail accounts
    once and returns the count of newly imported messages so the
    open page can decide whether to re-render.
  - The mobile client uses `POST /api/v1/mailbox/sync` instead
    (planned).

### `/billing/webhook` (Stripe → server)

- ✅ `POST /billing/webhook` — Stripe event envelope in, `2xx` out.
  Not part of the mobile contract; Stripe posts here directly.

### Auth (mobile)

- 🚧 `POST /api/v1/auth/login`
  - Body: `{"email": "...", "password": "..."}`.
  - `200` → `{"token": "<opaque>", "expiresAt": "<iso8601>",
    "user": {"id": 1, "email": "...", "displayName": "..."}}`.
  - `401 unauthorized` → `{"error": {"code": "bad_credentials",
    "message": "..."}}`.
  - Rate-limited by `LoginThrottleService` on the shared filter.

- 🚧 `POST /api/v1/auth/logout`
  - Header: `Authorization: Bearer <token>`.
  - Invalidates the token; `204`.

- 🚧 `POST /api/v1/auth/google`
  - Body: `{"idToken": "<google ID token>"}`.
  - Verifies the ID token against Google, provisions the user via
    `OAuth2ProvisioningService`, returns the same envelope as
    `/api/v1/auth/login`.

### Chats (mobile)

- 🚧 `GET /api/v1/chats?mailbox={address}&page={n}&size={m}`
  - `200` → `{"page": {"number": 0, "size": 20, "total": 137},
    "items": [ChatSummary]}`.
  - `ChatSummary` fields: `conversationKey` (string),
    `participants` (array of `{email, displayName?}`),
    `lastMessagePreview` (string), `lastMessageAt` (iso8601),
    `unread` (boolean), `mailbox` (string, the account address).

- 🚧 `GET /api/v1/chats/{conversationKey}`
  - `200` → `{"conversationKey": "...", "participants": [...],
    "messages": [Message]}` where `Message` is
    `{"id", "sentAt", "from", "to", "cc", "subject",
    "bodyHtml", "bodyText", "attachments": [...],
    "outbound": bool}`.
  - `404 not_found` if the key is unknown for the caller.

- 🚧 `POST /api/v1/chats/{conversationKey}/messages`
  - Body: `{"body": "..."}` (plain text; server wraps for HTML).
  - `201` → the newly-created outbound `Message`.
  - Server applies plan-tier send limits and enforces mailbox
    ownership before dispatching.

- 🚧 `POST /api/v1/chats/{conversationKey}/read`
  - `204`. Idempotent; marks the whole conversation read for the
    caller's active mailbox.

### Mailboxes (mobile)

- 🚧 `GET /api/v1/mailboxes` → `{"items": [MailAccountSummary]}`.
- 🚧 `POST /api/v1/mailbox/sync` → `{"imported": integer}` — mirrors
  `POST /chats/refresh` semantics for the mobile client.

### Account (mobile)

- 🚧 `GET /api/v1/me` → `{"id", "email", "displayName", "plan",
  "planStatus", "adminEmail": bool}` — the client's identity + plan
  context. Cheap probe used on app open.

### Live events

- 🚧 `GET /api/v1/events` (`text/event-stream`) — mobile parity of
  `/chats/events`, guarded by bearer token via a query param
  (`?token=`) because SSE cannot carry an `Authorization` header
  through most Dart HTTP clients. Emits `new-mail` events with
  `{"mailbox", "conversationKey"}` payloads.

## Error codes (initial)

| Code | Meaning |
| --- | --- |
| `bad_request` | Body validation failed. |
| `bad_credentials` | Email or password incorrect on `/auth/login`. |
| `unauthorized` | Missing or expired bearer token. |
| `forbidden` | Authenticated but not allowed (e.g. mailbox not owned). |
| `not_found` | Resource does not exist for this caller. |
| `plan_limit` | Blocked by plan tier — includes `limit_kind`. |
| `rate_limited` | Throttled; retry after `Retry-After`. |
| `internal` | Server error; log correlation id in `X-Request-Id`. |

## DTO stability

- Add fields freely — clients MUST ignore unknown fields.
- Never rename or repurpose an existing field. Add a new field and
  mark the old one ⛔ Deprecated with a target-removal date.
- Never change a field's type without adding a new field.

## Not yet decided (TBD)

- Attachment binary transport for the mobile client. The website
  currently stores `blob_ref = null` on `Attachment` rows and does
  not expose a download endpoint. Blocked on the roadmap item
  "Per-person shared files & links" in the root `CLAUDE.md`.
- Web Push / VAPID for background mail notifications. Tracked as
  "Live delivery, part 2" in the root `CLAUDE.md`.
