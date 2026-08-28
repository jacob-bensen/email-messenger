# ConexusMail JSON API — v1

**This document is the canonical contract between the ConexusMail backend and
its mobile client (`jacob-bensen/conexus-mail-mobile`).** Both sides depend
on the shapes below. Update this file *before* changing either side, and
never change one side without the other in the same run.

The web UI (Thymeleaf pages under `templates/`) is a separate surface — it
uses session cookies + CSRF and does not go through `/api/v1/**`. Both
surfaces share the same service layer; no business logic lives in a
controller.

- Base URL: derived from the deployment. Local dev: `http://localhost:8080/`.
- Content type: `application/json; charset=utf-8` for both request and
  response bodies (except explicitly binary responses).
- Authentication (unless a path is called out as public): every request
  carries `Authorization: Bearer <token>`. Tokens are opaque strings issued
  by `POST /api/v1/auth/login`. See §Auth.
- Errors: standard shape defined in §Errors.
- Versioning: the URL path is versioned (`/api/v1/…`). Breaking changes go
  behind `/api/v2/…`; additive changes stay in v1.

Sections are ordered by implementation priority.

## Auth

Authentication for the mobile client uses opaque bearer tokens. The backend
verifies the user's email + password (BCrypt via the existing
`UserService`), then persists a token row and returns the token. Every
subsequent JSON request sends the token as `Authorization: Bearer <token>`.

**Storage of tokens on the server:** `api_tokens` table (introduced in
Flyway `V36__api_tokens.sql`). Each row stores:

- `id` (bigserial)
- `user_id` (FK → `users`)
- `token_hash` (SHA-256 of the raw token, unique — the raw token is only
  ever returned to the client once, at issue time)
- `created_at`, `last_used_at`, `revoked_at` (nullable)
- `user_agent` (nullable; recorded from the login request for auditability)

**Expiry (v1):** tokens do not expire on the server. They are revoked when
the user signs out (`POST /api/v1/auth/logout`) or from the account UI.
A short-lived + refresh-token model is planned; see
`docs/INTERNAL_TODO.md`.

**Auth failures** return `401` with `{"error":"unauthorized"}`. When
`Authorization` is missing on a private route, the response is also `401`.

### `POST /api/v1/auth/login`  (public)

Log in with email + password, receive a bearer token.

Request:
```json
{"email": "ada@example.com", "password": "correct-horse-battery-staple"}
```

Response `200`:
```json
{
  "token": "cmxo_ax3f…opaque-string…",
  "user": {
    "id": "42",
    "email": "ada@example.com",
    "displayName": "Ada Lovelace",
    "plan": "PRO",
    "emailVerified": true
  }
}
```

Notes:
- `id` is the primary key of `users` as a string, so the client doesn't need
  to worry about 32/64-bit int handling.
- `plan` is one of `FREE`, `PRO`, `BUSINESS` (matching `Plan` enum). If the
  user has no `Subscription` row, the server treats the plan as `FREE`.
- `emailVerified` is `true` iff `users.email_verified_at IS NOT NULL`.

Errors:
- `400` — malformed JSON, missing fields.
- `401` — bad credentials (rate-limited by the same `LoginThrottleService`
  the form login uses).
- `403 {"error":"email_unverified"}` — reserved; not enforced in v1.

### `GET /api/v1/auth/me`  (requires bearer)

Return the current user, refreshed. Same `user` shape as the login response,
without the enclosing `token` key.

Response `200`:
```json
{
  "id": "42",
  "email": "ada@example.com",
  "displayName": "Ada Lovelace",
  "plan": "PRO",
  "emailVerified": true
}
```

### `POST /api/v1/auth/logout`  (requires bearer)

Revoke the bearer token used on the request (soft-delete — sets
`revoked_at`).

Response `204` (no body). Idempotent: revoking an already-revoked or
unknown token returns `204`.

## Chats  (planned — not yet implemented)

Kept here so the mobile client and backend agree on the target shape
before code lands.

### `GET /api/v1/chats`  → conversation list

Query params: `?page=0&size=50&filter=all|unread|mailbox&mailbox=<id>&q=<text>`.

Response `200`:
```json
{
  "page": 0,
  "size": 50,
  "total": 812,
  "items": [
    {
      "conversationKey": "ada@example.com,me@example.com",
      "participants": [
        {"id": "9", "email": "ada@example.com", "displayName": "Ada"}
      ],
      "lastMessage": {
        "id": "1042",
        "sentAt": "2026-08-28T18:04:11Z",
        "snippet": "sounds good — see you then",
        "outbound": false
      },
      "unread": 2,
      "messageCount": 7
    }
  ]
}
```

### `GET /api/v1/chats/{conversationKey}/messages`

Query params: `?before=<messageId>&limit=50`.

Returns messages oldest → newest inside the requested page. The client uses
`before` to page backwards.

### `POST /api/v1/chats/{conversationKey}/messages`

Send a reply (existing chat) or new message (fresh conversationKey). Body:

```json
{
  "clientMessageId": "5b9…-uuid",
  "mailboxId": "3",
  "subject": "Optional — server uses the thread's subject if omitted",
  "body": "Plain text body"
}
```

`clientMessageId` is echoed back in the response so the client can match its
optimistic bubble to the server's persisted row.

## Events  (planned)

### `GET /api/v1/events`  (text/event-stream)

SSE stream, parallel to the web's `/chats/events`. Emits `new-mail` events
with `{conversationKey, messageId}` payload.

## Mailboxes  (planned)

`GET /api/v1/mailboxes`, `POST /api/v1/mailboxes` (IMAP form),
`POST /api/v1/mailboxes/gmail/start` (returns the Google authz URL for
the client to open), `POST /api/v1/mailboxes/{id}/sync`,
`DELETE /api/v1/mailboxes/{id}`. Details deferred until we write the code.

## Errors

Non-2xx responses return JSON of the shape:

```json
{"error": "<slug>", "message": "human-readable string (optional)", "details": {}}
```

Slugs used so far: `bad_request`, `unauthorized`, `forbidden`, `not_found`,
`conflict`, `email_unverified`, `plan_limit`, `internal_error`.

Content-type on error is always `application/json`, even for `4xx` produced
by the framework (a JSON error handler is registered on the API filter
chain — separate from the HTML `GlobalExceptionHandler` used by the web
side).

## Non-goals for v1

- No user-registration through the JSON API. The mobile client links out to
  the web `/register` page in a browser tab. A native sign-up flow follows
  in a later milestone.
- No password reset through the JSON API — same reasoning; deep-link to
  `/password/forgot`.
- No admin endpoints on `/api/v1/**`; the operator dashboard stays
  Thymeleaf-only.
