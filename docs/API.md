# ConexusMail REST API — canonical contract

This file is the single source of truth for the JSON API the mobile client
(`conexus-mail-mobile`) talks to. The server implementation
(`com.emailmessenger.api`) and the client (`lib/data/`) must both match it
exactly; change this file first, then both sides, in the same run.

`MobileApiIntegrationTest` (backend) and `test/data/conexus_api_test.dart`
(client, using the example bodies below as fixtures) pin the contract.

## Conventions

- **Base path:** `/api/v1`. Everything under `/api/**` is JSON-only.
- **Auth:** `Authorization: Bearer <token>`. Tokens are opaque strings issued
  by `POST /auth/login`. No cookies or sessions are used or honoured on
  `/api/**`, and no CSRF token is needed.
- **Token lifetime:** 90 days from issue (`expiresAt`). A token dies early on
  `POST /auth/logout` (that token only), when the user's password is changed
  or reset on the website (all of the user's tokens), or when the account is
  disabled. There is no refresh endpoint — sign in again.
- **Timestamps:** ISO-8601 UTC instants with a `Z` suffix, e.g.
  `2026-01-03T09:30:00Z`.
- **Nulls:** nullable fields are always present and set to `null`; they are
  never omitted from responses. Optional *request* fields may be omitted.
- **The server is authoritative.** Every field in a response is computed
  server-side; clients display it and never recompute or predict it.

### Errors

Every non-2xx response from `/api/**` has this body:

```json
{ "error": { "code": "invalid_credentials", "message": "Incorrect email or password." } }
```

`code` is a stable machine-readable string; `message` is human-readable and
safe to show to the user.

| HTTP | `code`                | When                                                              |
|------|-----------------------|-------------------------------------------------------------------|
| 400  | `validation_failed`   | A field or query parameter failed validation.                     |
| 400  | `malformed_request`   | Body isn't valid JSON, or a parameter has the wrong type.         |
| 401  | `invalid_credentials` | Login only: wrong email/password, unknown or disabled account.    |
| 401  | `unauthorized`        | Missing, unknown, expired, or revoked bearer token.               |
| 403  | `forbidden`           | Authenticated but not allowed (no endpoint returns this yet).     |
| 429  | `too_many_attempts`   | Login only: too many recent failures for this email or IP.        |
| 500  | `internal_error`      | Unexpected server failure.                                        |

Clients must treat `401 unauthorized` on any call as "signed out": discard the
stored token and return to the sign-in screen.

Known gap: an unknown path under `/api/**` requested *with* a valid token
currently returns the website's HTML 404 page, not this envelope (tracked in
`INTERNAL_TODO.md`).

---

## Auth

### `POST /api/v1/auth/login`

Exchange an email + password for a bearer token. **Auth:** none.

Request:

```json
{ "email": "you@example.com", "password": "correct horse", "deviceName": "Pixel 9" }
```

| Field        | Type   | Rules                                                              |
|--------------|--------|--------------------------------------------------------------------|
| `email`      | string | Required, not blank, ≤ 254 chars. Trimmed and lower-cased server-side. |
| `password`   | string | Required, not blank, ≤ 200 chars.                                  |
| `deviceName` | string | Optional, ≤ 100 chars. A label for the token; not shown anywhere yet. |

`200 OK`:

```json
{
  "token": "q3vX0b1kYt8m2JcP5sHn7LwRz4dGfA9eUoT6iKyVxE0",
  "expiresAt": "2027-02-09T18:30:00Z",
  "user": {
    "id": 42,
    "email": "you@example.com",
    "displayName": "Your Name",
    "emailVerified": true
  }
}
```

`user` is a [User](#user). The token is shown exactly once; the server stores
only its SHA-256 hash.

Errors: `400 validation_failed`, `400 malformed_request`,
`401 invalid_credentials`, `429 too_many_attempts`.

Notes:

- The lockout is shared with the website's login form: 5 failures in 15
  minutes for the same email **or** the same IP locks both doors, and while
  locked even the correct password gets `429`.
- Accounts created with "Continue with Google" have no password and cannot
  sign in here until the user sets one via the website's password reset.
  (Google sign-in for mobile is not implemented.)
- Unverified email addresses can sign in; see `user.emailVerified`.

### `POST /api/v1/auth/logout`

Revoke the bearer token used on this request. **Auth:** bearer.

`204 No Content`, empty body. Other tokens of the same user stay valid.

Errors: `401 unauthorized`.

### `GET /api/v1/me`

The signed-in user. **Auth:** bearer.

`200 OK` — a [User](#user):

```json
{ "id": 42, "email": "you@example.com", "displayName": null, "emailVerified": false }
```

Errors: `401 unauthorized`.

#### User

| Field           | Type           | Notes                                         |
|-----------------|----------------|-----------------------------------------------|
| `id`            | integer        |                                               |
| `email`         | string         | Normalized (lower-case) login address.        |
| `displayName`   | string \| null | Null when never set.                          |
| `emailVerified` | boolean        | Whether the login address has been confirmed. |

---

## Conversations

A *conversation* is every email thread the user has with the same set of
people, collapsed into one chat (the website's `/chats`). It is identified by
`key`, which the server derives from the participant set.

### `GET /api/v1/conversations`

The chats list across all of the user's connected mailboxes, most recently
active first. **Auth:** bearer.

Calling this also asks the server to start a background refresh of any of the
user's mailboxes that are due for a poll (same as opening `/chats` on the
web); the response does not wait for it.

Query parameters:

| Param    | Type    | Default | Rules                                                                 |
|----------|---------|---------|-----------------------------------------------------------------------|
| `page`   | integer | `0`     | Zero-based, ≥ 0.                                                      |
| `size`   | integer | `30`    | 1–50.                                                                 |
| `q`      | string  | —       | Optional free-text search; blank is the same as absent.               |
| `filter` | string  | `all`   | One of `all`, `unread`, `awaiting`, `attachments` (case-insensitive). |

`200 OK`:

```json
{
  "items": [
    {
      "key": "3f9a1c0de4b72a55",
      "title": "Ada Lovelace, Bob",
      "initials": "AL",
      "group": true,
      "memberCount": 2,
      "preview": "Kickoff Monday",
      "lastActivityAt": "2026-01-03T09:30:00Z",
      "unread": true,
      "threadCount": 1,
      "accounts": ["you@example.com"],
      "senderEmail": "ada@acme.com"
    }
  ],
  "page": 0,
  "size": 30,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "counts": { "all": 2, "unread": 1, "awaiting": 0, "attachments": 0 }
}
```

| Field        | Type    | Notes                                                              |
|--------------|---------|--------------------------------------------------------------------|
| `items`      | array   | [ConversationSummary](#conversationsummary), in display order.     |
| `page`       | integer | Echo of the requested page.                                        |
| `size`       | integer | Echo of the requested page size.                                   |
| `totalItems` | integer | Conversations matching `q` + `filter`.                             |
| `totalPages` | integer |                                                                    |
| `hasNext`    | boolean | Whether `page + 1` has items.                                      |
| `counts`     | object  | Conversation totals per filter for the whole account — **not** narrowed by `q`, `filter`, or paging. Keys: `all`, `unread`, `awaiting`, `attachments` (integers). |

Because conversations reorder as mail arrives, a conversation can appear on
two consecutive pages if it moved between the two requests; clients
de-duplicate by `key` and keep the newer copy.

Errors: `400 validation_failed` (bad `page`/`size`/`filter`),
`400 malformed_request` (non-numeric `page`/`size`), `401 unauthorized`.

#### ConversationSummary

| Field            | Type           | Notes                                                                 |
|------------------|----------------|-----------------------------------------------------------------------|
| `key`            | string         | Conversation id; stable for a given set of people.                    |
| `title`          | string         | Display names (or addresses) of the other people.                     |
| `initials`       | string         | Avatar initials of the first member; `"?"` when there are none.       |
| `group`          | boolean        | True when there is more than one other person.                        |
| `memberCount`    | integer        | Other people in the conversation (the user is excluded).              |
| `preview`        | string         | Latest message as one line, ≤ ~120 chars; prefixed `"You: "` when the user sent it; `""` when unavailable. |
| `lastActivityAt` | string \| null | Instant of the latest message.                                        |
| `unread`         | boolean        | Whether any thread in the conversation is unread.                     |
| `threadCount`    | integer        | Email threads collapsed into this conversation.                       |
| `accounts`       | string[]       | The user's own addresses that took part; may be empty.                |
| `senderEmail`    | string \| null | Sender of the latest received message, else the primary correspondent. |

---

## Not yet in the API

These exist on the website only. The mobile roadmap
(`conexus-mail-mobile/docs/DESIGN.md` §ROADMAP) adds them in order:

- Reading a conversation's timeline and marking it read (`GET /chats/{key}`).
- Sending a message / reply, with attachments (`POST /chats/{key}/send`,
  `POST /chats/new`).
- Scoping the list to one mailbox (`?mailbox=` on the web).
- Live updates (the web uses SSE at `/chats/events`).
- Registration, password reset, email verification, Google sign-in.
- Mailbox connect/manage, saved searches, billing, teams.
