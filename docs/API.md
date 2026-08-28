# API — Canonical Contract for the Mobile Client

This document is the **single source of truth** for the JSON REST API
that `email-messenger` (backend) exposes to `conexus-mail-mobile`
(Flutter client). Both repos update this file in the **same commit** as
any implementation change. If the code and this file disagree, the code
is wrong.

## Status

**Bootstrap.** As of the first sync-routine run, the backend serves the
Thymeleaf website and one `@RestController` (the Stripe webhook). No
public JSON endpoints are exposed for the mobile client yet. This file
records the target shape so subsequent runs can add endpoints
incrementally, one vertical slice at a time.

The chat/mailbox/auth features described elsewhere in the codebase all
work today, but only through server-rendered pages. Mobile-facing
endpoints must be added deliberately: web pages and REST endpoints
share the same service layer, never a duplicated code path.

## Base URL & versioning

- Base path: `/api/v1`
- All requests and responses are `application/json; charset=utf-8`
  unless noted (SSE for `/api/v1/chats/stream` when implemented).
- Timestamps: RFC 3339 UTC (e.g. `2026-08-28T12:34:56Z`).
- IDs: opaque strings (never leak Postgres bigints; wrap as base62 or
  the equivalent).
- Breaking changes bump the base path (`/api/v2`) and keep the previous
  version deprecated for one full release before removal.

## Authentication

The web app uses session cookies + CSRF; that model does not fit a
mobile client. The mobile API uses **short-lived bearer tokens** with a
refresh token:

- `Authorization: Bearer <access_token>` on every authenticated request.
- Access token TTL: 15 minutes.
- Refresh token TTL: 30 days sliding, revoked on password change.
- Both tokens are opaque strings issued by the server; storage on the
  device uses the OS secure store (Keychain / EncryptedSharedPrefs).

CSRF is disabled for `/api/**` because tokens are not in cookies.

### `POST /api/v1/auth/login`

Body:
```json
{ "email": "you@example.com", "password": "…" }
```

Success (`200 OK`):
```json
{
  "accessToken": "…",
  "refreshToken": "…",
  "expiresIn": 900,
  "user": { "id": "u_…", "email": "you@example.com", "displayName": "You", "plan": "PRO" }
}
```

Errors: `401` invalid credentials, `403` email unverified, `429`
throttled (`LoginThrottleFilter`).

### `POST /api/v1/auth/refresh`

Body: `{ "refreshToken": "…" }` → same success shape as login.

### `POST /api/v1/auth/logout`

Header: `Authorization: Bearer …`. Revokes the refresh token. `204`.

### `POST /api/v1/auth/google`

Body: `{ "idToken": "…" }` — Google ID token from the mobile Sign-in
SDK. Server verifies against the same Google client used by
`GoogleOidcUserService` and provisions the user via
`OAuth2ProvisioningService`. Response mirrors `/auth/login`.

## User

### `GET /api/v1/me`

Header: `Authorization: Bearer …`. Returns the current user:
```json
{
  "id": "u_…",
  "email": "you@example.com",
  "displayName": "You",
  "plan": "PRO",
  "emailVerified": true,
  "teams": [ { "id": "t_…", "name": "Acme", "role": "OWNER" } ]
}
```

## Mail accounts

### `GET /api/v1/mailboxes`
Lists connected mail accounts (IMAP or Gmail OAuth). Fields:
`id`, `email`, `provider` (`IMAP`|`GMAIL_OAUTH`), `status`
(`OK`|`NEEDS_RECONNECT`|`ERROR`), `signature`.

### `POST /api/v1/mailboxes/imap`
Body: `{ "email", "host", "port", "username", "password",
        "sentFolder" }` → 201 with the created mailbox row.

### `POST /api/v1/mailboxes/gmail/start`
Returns `{ "authUrl": "…", "state": "…" }`. The mobile client opens
`authUrl` in a system web view; the redirect completes on the backend
web flow (existing `GmailConnectController` path) which stores the
tokens against the current user.

### `DELETE /api/v1/mailboxes/{id}`
Removes the mailbox and its cached messages. `204`.

## Chats (IM-style conversations)

The chats workspace is keyed by `conversationKey` (participant-set),
not `EmailThread`. The backend already computes this key
(`ConversationKeyService`, V32 migration).

### `GET /api/v1/chats?cursor=…&limit=50`
Returns the paginated chats rail. Sorted by most-recent bubble.
```json
{
  "items": [
    {
      "conversationKey": "ck_…",
      "participants": [ { "displayName": "Ada Lovelace", "email": "ada@…" } ],
      "lastBubble": { "senderId": "p_…", "preview": "See you then.",
                       "sentAt": "2026-08-28T09:12:00Z", "outbound": false },
      "unreadCount": 3
    }
  ],
  "nextCursor": "…"
}
```

### `GET /api/v1/chats/{conversationKey}?before=…&limit=100`
Returns bubbles for one conversation. Bubbles include one or more
`Message` ids collapsed by `IMTransformService`.

### `POST /api/v1/chats/{conversationKey}/messages`
Body: `{ "body": "…", "inReplyToMessageId": "m_…" }`. Sends via
`OutboundMessageService` / `ReplyService`. Returns the created bubble
with `status: "SENDING"` (roadmap: iMessage-style delivery states).

### `POST /api/v1/chats/{conversationKey}/read`
Marks the conversation read up to the most recent bubble. `204`.

### `GET /api/v1/chats/stream` (SSE)
Long-lived Server-Sent Events feed of `NewMailEvent` and read-state
changes, scoped to the authenticated user. Mirrors the existing
`/chats/events` stream used by the web workspace.

## Saved searches

### `GET /api/v1/saved-searches`
Lists the user's saved searches with `unreadCount`.

### `POST /api/v1/saved-searches`
Body: `{ "name", "query" }`. Plan-capped (`PlanLimitKind`).

### `DELETE /api/v1/saved-searches/{id}`

## Errors

All error responses share this envelope:
```json
{ "error": { "code": "INVALID_CREDENTIALS", "message": "Email or password is incorrect.",
             "field": "password" } }
```

Codes: `INVALID_CREDENTIALS`, `EMAIL_UNVERIFIED`, `THROTTLED`,
`PLAN_LIMIT`, `MAILBOX_ERROR`, `NOT_FOUND`, `CONFLICT`, `SERVER_ERROR`.

## Health

### `GET /api/v1/health`
Public, no auth. Returns `{ "status": "OK", "version": "0.1.0" }`.
The mobile client uses this to verify connectivity and the deployed
backend version.
