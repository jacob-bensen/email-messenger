# API

The canonical API contract between the backend (this repo) and the mobile
client (`jacob-bensen/conexus-mail-mobile`).

## Current honest state

**There is no JSON REST API for the mobile client yet.** The backend today
exposes only:

1. Thymeleaf HTML endpoints (form-encoded POSTs, HTML responses).
2. One SSE stream: `GET /chats/events` — server-sent events for live chat
   updates in the browser, session-cookie authenticated.
3. One JSON webhook: `POST /billing/webhook` — Stripe → server only, not
   for clients.

Everything below under **Planned API** is TBD until implemented. The
mobile client MUST NOT be built against this section until each endpoint
has a matching server implementation. Client and server changes ship in the
same run per the routine's Phase 1.

## Auth model (planned)

- Token: opaque bearer token issued to a device on sign-in; stored on the
  server as an `api_token` row keyed by user id and device id.
- Header: `Authorization: Bearer <token>` on every request.
- Refresh: a token has an idle expiry (e.g. 30 days) and is revoked on
  password change or explicit sign-out. Refresh flow TBD.
- The server remains authoritative for every mutation; the client sends
  intents, never final state.

## Media type

- Request `Content-Type: application/json; charset=utf-8`.
- Response `Content-Type: application/json; charset=utf-8`.
- All timestamps are RFC 3339 UTC (`2026-08-28T15:04:05Z`).
- All ids are opaque strings (server-generated).

## Error shape (planned)

```json
{
  "error": {
    "code": "not_found",
    "message": "Human-readable summary.",
    "details": { ... optional ... }
  }
}
```

Standard `code` values: `unauthorized`, `forbidden`, `not_found`,
`validation_failed`, `rate_limited`, `conflict`, `server_error`.

## Endpoints — current

### `GET /chats/events`

- Auth: session cookie (browser only today).
- Response: `text/event-stream`.
- Events (each `data:` line is JSON):
  - `{"type":"chat.updated","conversationKey":"<key>","at":"<rfc3339>"}`
  - `{"type":"chat.new","conversationKey":"<key>","preview":"<string>"}`

Reused by the mobile client once the bearer-token layer supports SSE
with `Authorization` (via query-string token fallback or a WS upgrade).

### `POST /billing/webhook`

Stripe → server. Not part of the client contract; documented here so a
future engineer doesn't try to call it from the app.

## Endpoints — planned (TBD)

Every entry below is a placeholder. Do NOT implement client code against
these until the corresponding backend endpoint exists and this row is
marked **implemented**. When you do implement one, replace the placeholder
with the actual request/response JSON schemas.

### Auth

- **[TBD]** `POST /api/v1/auth/login` — email + password → bearer token.
- **[TBD]** `POST /api/v1/auth/oauth/google` — Google ID token → bearer token.
- **[TBD]** `POST /api/v1/auth/logout` — revokes the calling token.
- **[TBD]** `GET  /api/v1/me` — current user profile + subscription summary.

### Chats

- **[TBD]** `GET  /api/v1/chats?cursor=<opaque>&limit=<n>` — paginated
  conversation-list view keyed by participant-set `conversationKey`.
- **[TBD]** `GET  /api/v1/chats/{conversationKey}` — full conversation with
  bubble runs.
- **[TBD]** `POST /api/v1/chats/{conversationKey}/messages` — send a reply.
  Body: `{ "body": "<markdown>", "inReplyToMessageId": "<optional>" }`.
- **[TBD]** `POST /api/v1/chats/{conversationKey}/read` — mark as read.

### Mailboxes

- **[TBD]** `GET  /api/v1/mailboxes` — connected mailbox list.
- **[TBD]** `POST /api/v1/mailboxes/imap` — connect an IMAP mailbox.
- **[TBD]** `POST /api/v1/mailboxes/gmail/oauth/start` — initiate OAuth flow.
- **[TBD]** `POST /api/v1/mailboxes/{id}/sync` — force a sync.
- **[TBD]** `POST /api/v1/mailboxes/{id}/delete` — disconnect.

### Live

- **[TBD]** `GET /api/v1/events` — SSE stream (bearer-token authenticated).
  Event shape mirrors `/chats/events`.

### Billing (mobile-visible read-only for now)

- **[TBD]** `GET /api/v1/subscription` — plan, status, trial end, portal URL.

## Change process

1. Update this file first — new endpoints, changed shapes, new error codes.
2. Implement the server (Spring controller + service + tests).
3. Implement the client (DTOs + networking layer + integration).
4. Bump `docs/DESIGN.md` §ROADMAP if a milestone shifts.
5. All three land in a single run per the routine's Phase 1 rule.
