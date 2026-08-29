# API.md — canonical REST contract (mobile ↔ server)

**Status:** DRAFT / MOSTLY TBD. As of the first Phase 0 pass the backend
exposes **no** REST API for the mobile client. The only non-HTML endpoint
today is `POST /billing/webhook` (Stripe). Everything else the website
uses is Thymeleaf form-POST + session cookie (see `docs/WEB.md`).

This file is the shared source of truth once endpoints exist. **Every
change here must land in both repos in the same run**: the Spring
controller/DTOs and the Dart client/DTOs must match this file exactly.

## Conventions (planned)

- Base path: `/api/v1`.
- Content type: `application/json; charset=utf-8` on both sides.
- Timestamps: RFC 3339 UTC strings (`2026-08-29T12:34:56Z`).
- IDs: opaque strings (the Java `Long` PK stringified) unless noted.
- Errors: uniform envelope
  ```json
  {"error": {"code": "STRING_ENUM", "message": "human readable"}}
  ```
  with an HTTP status from 4xx/5xx. `code` values: `UNAUTHENTICATED`,
  `FORBIDDEN`, `NOT_FOUND`, `VALIDATION`, `RATE_LIMITED`, `CONFLICT`,
  `INTERNAL`.
- Pagination: cursor-based. `?limit=N&cursor=OPAQUE`; responses return
  `nextCursor: string | null`.
- Auth: **TBD**. Two candidates on the table:
  1. Session cookie shared with the website (mobile embeds a WebView for
     login, then reuses the cookie). Simplest, keeps a single auth path.
  2. Per-device long-lived access token minted after form/OIDC login and
     sent as `Authorization: Bearer <token>`. Cleaner for native
     clients; requires a new token table + rotation.
  Server is authoritative either way — pick before shipping `/api/v1`.

## Endpoints (planned — none implemented yet)

### `POST /api/v1/auth/login` — TBD
Body: `{email, password}` **or** `{googleIdToken}`. Returns bearer token
or sets session cookie per the decision above.

### `GET /api/v1/mailboxes` — TBD
Returns the caller's connected mailboxes:
```json
{
  "mailboxes": [
    {"id":"1","address":"jane@example.com","authType":"IMAP_PASSWORD",
     "status":"ACTIVE","lastPolledAt":"2026-08-29T12:00:00Z"}
  ]
}
```

### `GET /api/v1/chats` — TBD
Cursor-paginated list of conversations for the authenticated user,
newest-first, keyed by `conversationKey`. Each entry:
```json
{
  "conversationKey":"a1b2…",
  "participants":[{"displayName":"Ada","address":"ada@example.com"}],
  "lastMessagePreview":"…",
  "lastMessageAt":"2026-08-29T12:00:00Z",
  "unreadCount":3
}
```

### `GET /api/v1/chats/{key}` — TBD
Full bubble stream for one conversation. Bubbles are the IM transform's
output (`ChatConversation` model): one bubble = one grouped run of
same-sender messages, with body already quote-stripped and markdown
rendered.

### `POST /api/v1/chats/{key}/send` — TBD
Body: `{body: "…", replyToMessageId?: "…"}`. Server composes and sends
via SMTP, threads the sent copy back onto `conversationKey`, and returns
the new bubble.

### `GET /api/v1/chats/events` — TBD
Server-sent events, mirror of the web `/chats/events` stream. Event
types: `mail.new`, `chat.updated`.

## Contract-change protocol

1. Update this file (endpoint, DTO, error codes).
2. In the backend, add/modify controllers + DTOs to match.
3. In the mobile client, mirror DTOs under `lib/domain/dto/` and update
   the network layer.
4. Both changes ship in the same run. If you cannot land both, revert
   the doc change until you can.
