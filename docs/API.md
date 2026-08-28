# API Contract — ConexusMail

Canonical contract between the `email-messenger` backend and the
`conexus-mail-mobile` Flutter client. **Both repos treat this file as
the source of truth.** Any change here must land in both
implementations in the same run (see the routine).

> Status legend
> - **IMPLEMENTED** — endpoint exists on the server today.
> - **PLANNED** — target contract; server does not expose it yet.
>
> As of 2026-08-28 the server has no dedicated REST API for the mobile
> client. Everything below marked PLANNED is the design we are building
> toward, endpoint by endpoint, in later runs. The Thymeleaf website
> already implements the underlying behavior through the shared service
> layer.

## Base URL

- Development: `http://localhost:8080`
- Production: `https://conexusmail.com` (subject to change once the
  mobile app ships)

All JSON endpoints live under the `/api/v1/` prefix. Older non-`/api`
paths are Thymeleaf pages and are **not** part of this contract.

## Transport & conventions

- Request and response bodies are `application/json; charset=utf-8`.
- Timestamps are ISO-8601 with offset (e.g. `2026-08-28T14:03:11Z`).
- IDs are the server's stable identifiers (numeric or opaque string).
  The mobile client MUST NOT synthesize or predict them.
- Errors follow the shape:
  ```json
  { "error": "invalid_request", "message": "human readable", "field": "email" }
  ```
  `error` is a stable, snake_case slug the client keys off. `field`
  is optional and only present for field-level validation errors.
- Standard HTTP status codes apply. `401` → mobile app must
  re-authenticate. `409` on state conflicts. `413` on oversized
  attachments (server enforces the same 10 MB / 25 MB caps configured
  in `application.yml`).

## Authentication

**PLANNED.** Bearer tokens issued by a dedicated mobile auth flow.
The website's session-cookie auth is not exposed to the mobile client.

### `POST /api/v1/auth/login`  *(PLANNED)*
Password login for the mobile client.

Request:
```json
{ "email": "user@example.com", "password": "…" }
```
Response `200`:
```json
{
  "accessToken": "…",
  "refreshToken": "…",
  "expiresIn": 3600,
  "user": { "id": 42, "email": "user@example.com", "displayName": "Ada" }
}
```
Errors: `401 invalid_credentials`, `403 email_unverified`,
`429 too_many_attempts` (mirrors `LoginThrottleService`).

### `POST /api/v1/auth/google`  *(PLANNED)*
Exchanges a Google ID token obtained by the mobile client's native
Google Sign-In for a ConexusMail access token. Same response shape as
`/auth/login`. Reuses `OAuth2ProvisioningService`.

### `POST /api/v1/auth/refresh`  *(PLANNED)*
Refreshes the access token. Request `{ "refreshToken": "…" }`,
response is the same shape as login.

### `POST /api/v1/auth/logout`  *(PLANNED)*
Revokes the current refresh token.

## Account

### `GET /api/v1/me`  *(PLANNED)*
Returns the authenticated user, plan, and feature flags relevant to
the mobile client.

Response `200`:
```json
{
  "id": 42,
  "email": "user@example.com",
  "displayName": "Ada",
  "plan": "FREE",
  "features": {
    "imapIdle": false,
    "webPush": true
  }
}
```

## Mailboxes

### `GET /api/v1/mailboxes`  *(PLANNED)*
Lists mailboxes connected to the authenticated user. Mirrors
`MailboxView`.

Response `200`:
```json
{
  "mailboxes": [
    {
      "id": 7,
      "email": "ada@example.com",
      "provider": "GMAIL_OAUTH",
      "displayName": "Ada (Gmail)",
      "authType": "OAUTH",
      "connected": true,
      "lastSyncAt": "2026-08-28T14:03:11Z",
      "unreadCount": 3
    }
  ]
}
```

### `POST /api/v1/mailboxes/{id}/sync`  *(PLANNED)*
Triggers a manual poll. Mirrors the existing
`POST /mailboxes/{id}/sync`. Response `202`:
```json
{ "queued": true }
```

## Conversations (chats)

Chats are grouped by participant-set `conversationKey` — the same
concept the website uses. `key` in the URL is that
`conversationKey`.

### `GET /api/v1/chats`  *(PLANNED)*
Paginated list of conversations for the authenticated user, newest
first. Mirrors `ConversationListService`.

Query params:
- `limit` (default 30, max 100)
- `cursor` (opaque; from a previous response's `nextCursor`)
- `filter` (`all` \| `unread` \| `starred`)

Response `200`:
```json
{
  "items": [
    {
      "key": "ada@example.com,ben@example.com",
      "participants": [
        { "email": "ada@example.com", "displayName": "Ada" },
        { "email": "ben@example.com", "displayName": "Ben" }
      ],
      "lastMessagePreview": "See you at 3.",
      "lastMessageAt": "2026-08-28T14:03:11Z",
      "unread": true,
      "unreadCount": 2
    }
  ],
  "nextCursor": null
}
```

### `GET /api/v1/chats/{key}`  *(PLANNED)*
Full conversation view: the IM-transformed bubble runs.

Response `200`:
```json
{
  "key": "ada@example.com,ben@example.com",
  "participants": [ … ],
  "runs": [
    {
      "sender": { "email": "ada@example.com", "displayName": "Ada" },
      "self": false,
      "messages": [
        {
          "id": 1234,
          "sentAt": "2026-08-28T14:00:00Z",
          "bodyHtml": "<p>Hey!</p>",
          "bodyText": "Hey!",
          "attachments": [
            { "id": 88, "filename": "photo.jpg", "sizeBytes": 12345, "mimeType": "image/jpeg" }
          ]
        }
      ]
    }
  ]
}
```

### `POST /api/v1/chats/{key}/send`  *(PLANNED)*
Sends a reply. Multipart if attachments are present, JSON otherwise.

JSON body:
```json
{ "bodyHtml": "<p>Ok.</p>", "bodyText": "Ok." }
```
Response `202`:
```json
{ "messageId": 1290, "status": "SENDING" }
```
Server enforces plan limits. The response `status` will progress to
`SENT` and later `DELIVERED` / `NOT_DELIVERED` — see the delivery-state
roadmap item in the backend `CLAUDE.md`. Clients SHOULD reconcile via
the events stream (below).

### `POST /api/v1/chats/refresh`  *(IMPLEMENTED — HTML session only)*
The website endpoint already exists; the mobile equivalent will be
added under `/api/v1/`.

## Real-time events

### `GET /api/v1/events`  *(PLANNED)*
Server-Sent Events stream for the authenticated user; mirrors the
website's `/chats/events`. Event types:
- `new_mail` — payload includes `conversationKey`.
- `message_status` — payload `{ messageId, status }` for outbound
  delivery-state transitions.

Mobile clients that cannot hold SSE open in the background will use
Web Push (planned) once VAPID keys are configured.

## Error slugs (extend as endpoints land)

| Slug | Meaning |
| --- | --- |
| `invalid_request` | Malformed body or missing required field. |
| `invalid_credentials` | Login failed. |
| `email_unverified` | Login blocked pending verification. |
| `too_many_attempts` | Login throttled. |
| `unauthorized` | Missing/expired access token. |
| `plan_limit_reached` | Action blocked by the user's plan. |
| `not_found` | Target resource does not exist for this user. |
| `conflict` | Concurrent update. |

## Change log for this contract

- 2026-08-28: Initial draft. No endpoints implemented on the server
  yet; documented target shape.
