# API.md — the canonical contract for `conexus-mail-mobile`

**This file is the single source of truth for the JSON API that the mobile
client consumes.** The backend and the Flutter client must both match this
document exactly. Update this file BEFORE implementing any change on
either side.

## Current status

**The REST/JSON API for the mobile client does not yet exist.** Today the
backend serves only:

- The public website via Thymeleaf (`text/html`) — see `docs/WEB.md`.
- `POST /billing/webhook` — Stripe webhook receiver (`@RestController`).
- `GET /chats/events` — server-sent events for the logged-in web session
  (cookie auth); not intended for mobile.
- `GET /manifest.webmanifest`, `GET /sw.js`, `GET /apple-touch-icon.png`,
  etc. — PWA static assets.

Everything below §Planned surface is **not yet implemented**. The routine
will build it out incrementally, one feature at a time, in lockstep with
the mobile client.

## Conventions (planned)

- **Base path**: `/api/v1`
- **Content-type**: `application/json; charset=utf-8` for both
  request and response bodies.
- **Time**: ISO-8601 UTC, e.g. `"2026-08-28T14:23:11Z"`.
- **IDs**: server-generated `long` in JSON as a number; `conversationKey`
  is an opaque `string` (server derives it from the participant set).
- **Errors**: standard shape
  ```json
  { "error": { "code": "PLAN_LIMIT_EXCEEDED", "message": "…", "details": { … } } }
  ```
  HTTP status matches the class (`400` validation, `401` unauthenticated,
  `403` forbidden / plan limit, `404` missing, `409` conflict,
  `422` unprocessable, `429` throttled, `5xx` server).
- **Pagination**: cursor-based via `?cursor=<opaque>&limit=<n≤100>`;
  response includes `"nextCursor": "…"` or `null`.
- **Idempotency**: unsafe writes accept optional
  `Idempotency-Key: <uuid>` header; server dedupes for 24h.

## Auth (planned)

The mobile client authenticates with a bearer token issued by the server:

- `POST /api/v1/auth/login` — `{email, password}` → `{accessToken, refreshToken, user}`.
- `POST /api/v1/auth/refresh` — `{refreshToken}` → new `{accessToken, refreshToken}`.
- `POST /api/v1/auth/google` — `{idToken}` (from Google Sign-In SDK) →
  same as `/login`.
- `POST /api/v1/auth/logout` — revokes the refresh token.

All authenticated calls send `Authorization: Bearer <accessToken>`.
Access tokens are short-lived (15 min) JWT; refresh tokens are opaque and
rotated on every refresh.

Server-side, tokens are minted by a new `auth.token.*` package. Login
still runs through `AccountService` / `UserService` — same rules as the
form login (email verified, not locked out).

## Planned surface (roadmap; nothing here is implemented yet)

Each endpoint below has a status tag:
- `TBD` — designed here but not implemented on either side.
- `SERVER` — implemented on the backend only.
- `CLIENT` — wired on the mobile only.
- `LIVE` — implemented and shipped end-to-end.

### Session & account

| Method | Path                              | Status | Notes |
| ------ | --------------------------------- | ------ | ----- |
| GET    | `/api/v1/me`                      | TBD    | Current user + plan + feature flags. |
| PATCH  | `/api/v1/me`                      | TBD    | Display name, appearance settings, notification prefs. |
| POST   | `/api/v1/me/password`             | TBD    | Change password. |

### Mailboxes

| Method | Path                              | Status | Notes |
| ------ | --------------------------------- | ------ | ----- |
| GET    | `/api/v1/mailboxes`               | TBD    | List connected mailboxes. |
| POST   | `/api/v1/mailboxes`               | TBD    | IMAP connect: `{provider, host, port, username, password}`. |
| POST   | `/api/v1/mailboxes/gmail/authorize` | TBD  | Returns Google OAuth URL; client opens in a browser tab; callback lands on the server. |
| POST   | `/api/v1/mailboxes/{id}/sync`     | TBD    | Trigger an immediate poll. |
| DELETE | `/api/v1/mailboxes/{id}`          | TBD    | Disconnect. |

### Chats (conversations)

| Method | Path                              | Status | Notes |
| ------ | --------------------------------- | ------ | ----- |
| GET    | `/api/v1/chats`                   | TBD    | Paginated chat list (participant-set conversationKey), sorted by last-message desc. Query: `?cursor=…&limit=…&unread=true` |
| GET    | `/api/v1/chats/{key}`             | TBD    | Full conversation view: bubble runs + participants + attachments (metadata only). |
| POST   | `/api/v1/chats/{key}/messages`    | TBD    | Send a reply. Body: `{textBody, htmlBody?, inReplyToMessageId?}`. Server-authoritative threading. |
| POST   | `/api/v1/chats/{key}/read`        | TBD    | Mark as read. |
| POST   | `/api/v1/chats/new`               | TBD    | Start a new chat with a participant set. |

### Search

| Method | Path                              | Status | Notes |
| ------ | --------------------------------- | ------ | ----- |
| GET    | `/api/v1/search?q=…`              | TBD    | Full-text search across chats. |
| GET    | `/api/v1/saved-searches`          | TBD    | List. |
| POST   | `/api/v1/saved-searches`          | TBD    | `{name, query}`. |
| DELETE | `/api/v1/saved-searches/{id}`     | TBD    | Delete. |

### Live delivery (SSE)

| Method | Path                              | Status | Notes |
| ------ | --------------------------------- | ------ | ----- |
| GET    | `/api/v1/events`                  | TBD    | `text/event-stream`; auth via `Authorization` header OR `?access_token=…` query for platforms that can't set headers on EventSource. Event kinds: `chat.new`, `chat.updated`, `chat.read`. |

### Billing

Mobile billing uses the native platform SDKs (App Store / Play Store) for
in-app purchase; the server verifies purchase tokens through a
provider-specific verifier. The Stripe web flow stays unchanged. This
surface is **entirely TBD** and will be spec'd when we start on billing.

## Shared DTO shapes (planned)

### `User`
```json
{
  "id": 42,
  "email": "alice@example.com",
  "displayName": "Alice",
  "plan": "PRO",
  "planStatus": "ACTIVE",
  "trialEndsAt": null,
  "features": { "livePush": true, "attachments": false }
}
```

### `ChatSummary`
```json
{
  "conversationKey": "abc123…",
  "participants": [{"email":"…","displayName":"…","avatarUrl":null}, …],
  "lastMessageAt": "2026-08-28T14:23:11Z",
  "lastMessagePreview": "See you at 3.",
  "unread": true,
  "unreadCount": 3
}
```

### `ChatDetail`
```json
{
  "conversationKey": "abc123…",
  "participants": [ … ],
  "bubbleRuns": [
    { "sender": {"email":"…","displayName":"…","avatarUrl":null},
      "messages": [
        { "id": 101, "sentAt":"…", "textBody":"…", "htmlBody":"…",
          "outbound": false, "attachments": [ … ], "threadId": 7 }
      ] }
  ],
  "nextCursor": null
}
```

### `Error`
```json
{ "error": { "code": "PLAN_LIMIT_EXCEEDED",
             "message": "Free plan is limited to 1 mailbox.",
             "details": { "limit": 1 } } }
```

## Change log for the contract

Every substantive change here must add an entry:

| Date       | Change |
| ---------- | ------ |
| 2026-08-28 | Initial doc created; no API implemented yet. All endpoints TBD. |
