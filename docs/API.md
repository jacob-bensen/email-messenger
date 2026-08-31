# API.md — ConexusMail API contract

**Status: DRAFT (2026-08-31).** This is intended as the canonical contract
between the backend and the mobile client (`conexus-mail-mobile`). Today,
almost none of it is implemented — the ConexusMail server is currently
Thymeleaf-only. This document is the shared source of truth for what a
future mobile-facing JSON API will look like; both repos must stay in sync
with it. The "Implemented" column tells you what actually exists in code
right now vs. what is planned.

Any change to a documented shape must land in both repositories in the
same commit (this file + backend implementation + mobile client code).

## Base URL / transport

- Base URL: `https://<host>/api/v1`
- Content-Type: `application/json; charset=utf-8` on requests and responses
  that carry a body (SSE is `text/event-stream`).
- All timestamps are RFC 3339 UTC (`2026-08-31T14:00:00Z`).
- All IDs are stringified 64-bit integers (Dart's JSON `int` handles them
  safely up to 2^53; server IDs will never exceed that in practice).
- Errors use the shape:
  ```json
  { "error": { "code": "invalid_credentials", "message": "..." } }
  ```
  with `code` a machine-stable string and `message` human-friendly. HTTP
  status codes are standard (400 validation, 401 auth, 403 forbidden, 404
  missing, 409 conflict, 429 rate-limited, 5xx server).

## Authentication

The server today uses session cookies + CSRF for the Thymeleaf website.
For the mobile client we will add **bearer-token** authentication on
`/api/**` paths. Tokens are issued at sign-in and refreshed via a refresh
token. Session cookies are NOT used on `/api/**`.

| Endpoint                    | Method | Auth       | Implemented |
| --------------------------- | ------ | ---------- | ----------- |
| `/api/v1/auth/sign-in`      | POST   | none       | ❌ planned  |
| `/api/v1/auth/refresh`      | POST   | refresh    | ❌ planned  |
| `/api/v1/auth/sign-out`     | POST   | bearer     | ❌ planned  |
| `/api/v1/auth/register`     | POST   | none       | ❌ planned  |
| `/api/v1/auth/google/mobile`| POST   | none       | ❌ planned  |

### `POST /api/v1/auth/sign-in`

Request:
```json
{ "email": "you@example.com", "password": "..." }
```

Response 200:
```json
{
  "accessToken": "eyJ...",
  "accessTokenExpiresAt": "2026-08-31T15:00:00Z",
  "refreshToken": "opaque-string",
  "user": { "id": "1", "email": "you@example.com", "displayName": "..." }
}
```

Errors: 401 `invalid_credentials`, 403 `email_not_verified`, 429
`rate_limited`.

### `POST /api/v1/auth/refresh`

Request: `{ "refreshToken": "..." }`
Response 200: same shape as sign-in (new access token; refresh token MAY
be rotated).

## Mailboxes

| Endpoint                              | Method | Auth   | Implemented |
| ------------------------------------- | ------ | ------ | ----------- |
| `/api/v1/mailboxes`                   | GET    | bearer | ❌ planned  |
| `/api/v1/mailboxes`                   | POST   | bearer | ❌ planned  |
| `/api/v1/mailboxes/{id}`              | DELETE | bearer | ❌ planned  |
| `/api/v1/mailboxes/{id}/sync`         | POST   | bearer | ❌ planned  |
| `/api/v1/mailboxes/{id}/signature`    | PUT    | bearer | ❌ planned  |
| `/api/v1/mailboxes/gmail/oauth/start` | GET    | bearer | ❌ planned  |

`GET /api/v1/mailboxes` returns:
```json
{
  "mailboxes": [
    {
      "id": "12",
      "address": "you@example.com",
      "authType": "IMAP_PASSWORD",
      "syncFrequencyMinutes": 15,
      "signature": "— Jacob",
      "lastSyncedAt": "2026-08-31T13:57:00Z",
      "state": "OK"
    }
  ],
  "defaultMailAccountId": "12"
}
```

`authType` ∈ {`IMAP_PASSWORD`, `GMAIL_OAUTH`}. `state` ∈ {`OK`,
`RECONNECT_REQUIRED`, `ERROR`}.

## Chats

| Endpoint                                    | Method | Auth   | Implemented |
| ------------------------------------------- | ------ | ------ | ----------- |
| `/api/v1/chats`                             | GET    | bearer | ❌ planned  |
| `/api/v1/chats/{conversationKey}`           | GET    | bearer | ❌ planned  |
| `/api/v1/chats/{conversationKey}/messages`  | POST   | bearer | ❌ planned  |
| `/api/v1/chats/{conversationKey}/read`      | POST   | bearer | ❌ planned  |
| `/api/v1/chats/events`                      | GET    | bearer | ❌ planned  |

`GET /api/v1/chats` — paginated list. Query params: `?cursor=...&limit=50`.
```json
{
  "chats": [
    {
      "conversationKey": "abc123",
      "participants": [
        { "address": "ada@example.com", "displayName": "Ada" }
      ],
      "lastMessagePreview": "See you then",
      "lastMessageAt": "2026-08-31T13:50:00Z",
      "unread": true,
      "unreadCount": 2
    }
  ],
  "nextCursor": "..."
}
```

`GET /api/v1/chats/{conversationKey}` — one conversation view (bubbles).
Response includes `messages[]` ordered oldest-first, each with `id`,
`fromParticipant`, `sentAt`, `outbound`, `bodyPlain`, `bodyHtml`,
`attachments[]` (metadata only until blob storage lands).

`POST /api/v1/chats/{conversationKey}/messages` — send a reply. Request:
```json
{ "bodyPlain": "Sounds good.", "fromMailAccountId": "12",
  "inReplyToMessageId": "999" }
```
Response 201: the created message DTO.

`GET /api/v1/chats/events` — SSE stream, mirrors `/chats/events`. Events:
- `new-mail` — payload `{"imported": <int>}`, fired after new inbound.
- `keepalive` (SSE comment) every 25s.

## Account

| Endpoint                              | Method | Auth   | Implemented |
| ------------------------------------- | ------ | ------ | ----------- |
| `/api/v1/account`                     | GET    | bearer | ❌ planned  |
| `/api/v1/account/preferences`         | PUT    | bearer | ❌ planned  |
| `/api/v1/account/notifications`       | PUT    | bearer | ❌ planned  |
| `/api/v1/account/push-subscriptions`  | POST   | bearer | ❌ planned  |
| `/api/v1/account/push-subscriptions/{id}` | DELETE | bearer | ❌ planned |

## What actually exists today (server-rendered)

The current server does NOT expose the endpoints above. What it does have,
usable in a mobile embedded webview or as raw HTML:

| Endpoint                | Method | Auth    | Returns                              |
| ----------------------- | ------ | ------- | ------------------------------------ |
| `POST /billing/webhook` | POST   | Stripe  | Stripe webhook — not for clients.    |
| `POST /chats/refresh`   | POST   | session | JSON `{"imported": <int>}`           |
| `GET /chats/events`     | GET    | session | SSE `new-mail` events                |
| `GET /manifest.webmanifest` | GET | none  | PWA manifest                         |

Everything else is HTML. See `docs/WEB.md` for the full page inventory.

## Versioning

- `/api/v1/...` is the current major version. Breaking changes require
  a new `/api/v2/...` root.
- Additive changes (new endpoint, new optional field) are backwards
  compatible and don't bump the major.

## Change log

- 2026-08-31 — Initial contract draft. Nothing implemented yet.
