# API — REST contract for the mobile client

**This file is the canonical wire contract shared between the backend
(`jacob-bensen/email-messenger`) and the Flutter mobile client
(`jacob-bensen/conexus-mail-mobile`).** Any change to a schema or route
here must be accompanied, in the same commit set, by matching changes to
both the Spring controllers and the Dart DTOs. The server is
authoritative for all state and business logic; the client mirrors it.

Base path: `/api/v1` (versioned; a breaking change bumps to `/api/v2`).

## Authentication

For the moment the API accepts the same session cookie the website uses
(`JSESSIONID` + optional `remember-me`). CSRF is disabled for `/api/**`
because a mobile client cannot easily attach the Thymeleaf CSRF token —
which means the API **only exposes GET endpoints today**. State-mutating
endpoints (POST/PUT/DELETE) are gated on shipping a proper bearer-token
mechanism (OAuth2 password grant or personal-access-tokens); see
`INTERNAL_TODO.md`.

An unauthenticated request to any `/api/**` route returns HTTP 401
(empty body). This differs from the Thymeleaf pages, which 302 to
`/login`. The mobile client treats 401 as "session expired,
re-authenticate."

## Media type

Requests and responses use `application/json; charset=utf-8`. Timestamps
are ISO-8601 UTC (`2026-08-29T14:30:00Z`) when serialized as strings,
and epoch millis when serialized as a number (each field's schema notes
which).

## Pagination

List endpoints return an envelope:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 30,
  "totalElements": 128,
  "totalPages": 5,
  "hasNext": true
}
```

`page` is 0-indexed. Client passes `?page=N&size=M`; server clamps
`size` to `[1, 100]`.

## Error format

Errors return the standard Spring Boot problem body plus a `code` field
when the server can classify it:

```json
{ "status": 400, "error": "Bad Request", "code": "SIZE_TOO_LARGE",
  "message": "size must be <= 100" }
```

## Endpoints

### `GET /api/v1/chats`

List the authenticated user's chats (conversations), most-recently-active
first. Delegates to `ConversationListService.list(...)` — the same
service backing the `/chats` web view.

Query parameters:

| name    | type    | default | description                                          |
|---------|---------|---------|------------------------------------------------------|
| `page`  | int     | `0`     | 0-indexed page                                       |
| `size`  | int     | `30`    | rows per page, clamped to `[1, 100]`                 |
| `q`     | string  | (none)  | free-text query (matches title / preview)            |
| `filter`| enum    | `ALL`   | one of `ALL`, `UNREAD`, `AWAITING`, `ATTACHMENTS`    |
| `mailbox`| string | (none)  | scope to one connected account address; omit for Hub |

Response `200 OK`, paginated envelope of `ChatSummary`:

```json
{
  "content": [
    {
      "key": "alice@example.com|bob@example.com",
      "title": "Alice, Bob",
      "initials": "A",
      "group": true,
      "memberCount": 2,
      "preview": "You: sounds good, thursday works",
      "lastActivity": "2026-08-29T13:22:04Z",
      "lastActivityEpochMillis": 1787147324000,
      "unread": false,
      "threadCount": 3,
      "accounts": ["me@example.com"],
      "senderEmail": "alice@example.com"
    }
  ],
  "page": 0, "size": 30, "totalElements": 42, "totalPages": 2, "hasNext": true
}
```

`ChatSummary` field notes:

- `key` — opaque conversation key (sorted, normalized participant set).
  Used as the path parameter for future single-chat endpoints. **Never
  parse this on the client.** Treat it as an opaque string.
- `title` — display label already assembled server-side.
- `initials` — one-letter fallback for the avatar.
- `group` — true when the chat has 2+ non-owner participants.
- `preview` — plain-text one-liner (already prefixed with `"You: "` for
  outbound). Max ~120 chars.
- `lastActivity` — ISO-8601 UTC string, nullable if the chat has no
  messages yet (won't happen in practice — filtered out).
- `lastActivityEpochMillis` — same instant as an integer, for cheap
  client sorting.
- `unread` — true if any thread in the chat has unread messages for the
  owner.
- `threadCount` — how many `EmailThread` rows collapse into this chat.
- `accounts` — the owner's own connected-account addresses this chat was
  received on; only populated when `mailbox` is omitted (the cross-account
  Hub). Empty list otherwise.
- `senderEmail` — the sender of the most recent message (or the primary
  correspondent when the last message was outbound). Shown in grey after
  the title in the UI.

## Planned endpoints (not yet implemented)

These are listed so both repos plan against the same shape. Each will
land through the same "controller → shared service → mapper" pattern.

- `GET /api/v1/chats/{key}` — a single chat's unified timeline
  (bubbles, senders, day separators).
- `GET /api/v1/chats/{key}/messages?before=...` — cursor-based scroll
  for older messages.
- `POST /api/v1/chats/{key}/reply` — send a reply on the chat's most
  recent thread. **Blocked on bearer-token auth.**
- `GET /api/v1/mailboxes` — the user's connected accounts.
- `GET /api/v1/me` — profile, plan, quota.
