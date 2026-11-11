# ConexusMail REST API — v1

**This file is the canonical contract** between the backend (this repo) and the
mobile client ([`conexus-mail-mobile`](https://github.com/jacob-bensen/conexus-mail-mobile)).
Change it *first*, then change both implementations in the same run. The
client's test fixtures (`test/fixtures.dart`) are copied from the examples here.

Implemented in `com.emailmessenger.api` (controllers, DTOs) and
`com.emailmessenger.auth` (`ApiSecurityConfig`, `ApiTokenService`). Covered by
`ApiAuthControllerTest` and `ApiConversationControllerTest`.

## Conventions

- Base path: `/api/v1`. Requests and responses are `application/json`, UTF-8.
- **Authentication:** `Authorization: Bearer <token>` on every endpoint except
  `POST /auth/login`. Tokens are opaque strings starting with `cxm_`, issued by
  login, valid for **90 days**. A token stops working when it expires, when it
  is used for `POST /auth/logout`, or when the account's password is changed or
  reset (every token for that user is revoked). The server stores only a
  SHA-256 of the token.
- The API is stateless: no cookies, no session, no CSRF token. Browser session
  cookies are **not** accepted on `/api/**`, and bearer tokens are **not**
  accepted on the website's routes.
- **Timestamps** are ISO-8601 UTC instants (`2026-01-02T09:00:00Z`). A
  timestamp field may be `null` where noted.
- **Ids** are JSON integers (64-bit). A conversation `key` is an opaque string
  computed by the server from the conversation's participant set — never parse
  or construct one.
- Fields documented as nullable are always present, with value `null`.
- Unknown query parameters are ignored. Clients must ignore unknown response
  fields (fields may be added without a version bump).

## Errors

Every non-2xx response from an API endpoint has this body:

```json
{ "error": { "code": "invalid_credentials", "message": "That email and password don't match." } }
```

`message` is human-readable and safe to display. Branch on `code`:

| HTTP | `code`               | When |
|------|----------------------|------|
| 400  | `validation_failed`  | Missing/blank/oversized field, malformed JSON, wrong parameter type. |
| 401  | `invalid_credentials`| Login only: unknown email, wrong password, or disabled account (indistinguishable on purpose). |
| 401  | `unauthorized`       | Missing, malformed, expired, or revoked bearer token. The client must discard its token and show the login screen. |
| 403  | `forbidden`          | Authenticated but not allowed. (No endpoint returns this yet.) |
| 404  | `not_found`          | The resource doesn't exist *for this user* (other users' data is reported as not found). |
| 405  | `method_not_allowed` | Wrong HTTP method on a known path. |
| 429  | `too_many_attempts`  | Login only: 5 failed logins for the same email or the same IP within 15 minutes. Shared with the website's login throttle. |
| 500  | `internal_error`     | Unexpected server failure. |

Known gap: a request to a path that matches no endpoint at all (e.g.
`/api/v1/nope`) currently gets the website's HTML 404 page rather than this
envelope — see `INTERNAL_TODO.md`.

---

## Auth

### `POST /api/v1/auth/login` — no auth

Exchanges an email + password for a bearer token. Email matching is
case-insensitive. Goes through the same credential check, failure throttle, and
audit log (`auth_events`) as the website's form login.

Accounts created with "Continue with Google" have no password until one is set
from **Account** on the website; until then login returns `invalid_credentials`.

Request:

```json
{ "email": "owner@example.com", "password": "hunter22", "deviceName": "Pixel 9" }
```

| Field        | Type   | Rules |
|--------------|--------|-------|
| `email`      | string | required, non-blank, ≤ 254 chars |
| `password`   | string | required, non-blank, ≤ 200 chars |
| `deviceName` | string | optional, ≤ 100 chars; stored with the token for a future "signed-in devices" list |

`200 OK`:

```json
{
  "token": "cxm_test-token",
  "expiresAt": "2027-02-09T20:15:00Z",
  "user": {
    "id": 7,
    "email": "owner@example.com",
    "displayName": "Owner",
    "emailVerified": true,
    "plan": "pro"
  }
}
```

`user` is a [User](#user). Errors: `400 validation_failed`,
`401 invalid_credentials`, `429 too_many_attempts`.

### `POST /api/v1/auth/logout`

Revokes the token the request was made with. No body. `204 No Content`.
Other tokens for the same user keep working. Errors: `401 unauthorized`.

### `GET /api/v1/me`

`200 OK` with a [User](#user). Errors: `401 unauthorized`.

#### User

| Field           | Type           | Notes |
|-----------------|----------------|-------|
| `id`            | integer        | |
| `email`         | string         | Login email, lower-cased. |
| `displayName`   | string \| null | |
| `emailVerified` | boolean        | |
| `plan`          | string         | `free`, `pro`, or `business` — the plan the user is currently *entitled* to (an inactive subscription reports `free`). |

---

## Conversations

A *conversation* is every message exchanged with one person or one group (the
same participant set), merged across email threads — the texting-style unit the
website shows at `/chats`.

### `GET /api/v1/conversations`

The chats list, most recently active first. Also triggers the same
non-blocking refresh of due mailboxes that opening `/chats` does.

| Query param | Type    | Default | Notes |
|-------------|---------|---------|-------|
| `page`      | integer | `0`     | Zero-based; negative values are treated as `0`. |
| `size`      | integer | `30`    | Clamped to `1…100`. |
| `q`         | string  | —       | Free-text search (same matching as the website's search box). |
| `filter`    | string  | `all`   | `all`, `unread`, `awaiting` (the last message is from them — you owe a reply), `attachments`. Case-insensitive; unrecognized values behave as `all`. |
| `mailbox`   | integer | —       | Id of one of the user's connected mail accounts, to scope the list to it. Omitted, invalid, or when the user has only one account: all accounts. |

`200 OK`:

```json
{
  "items": [
    {
      "key": "k-ada",
      "title": "Ada Lovelace",
      "initials": "AL",
      "group": false,
      "memberCount": 1,
      "preview": "About the invoice.",
      "lastActivityAt": "2026-01-01T09:00:00Z",
      "unread": true,
      "threadCount": 1,
      "accounts": ["owner@example.com"],
      "senderEmail": "ada@acme.com"
    }
  ],
  "page": 0,
  "size": 30,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false
}
```

#### ConversationSummary

| Field            | Type            | Notes |
|------------------|-----------------|-------|
| `key`            | string          | Opaque conversation id. |
| `title`          | string          | The other participants' names, e.g. `"Ada Lovelace, Bob Stone"`. |
| `initials`       | string          | For an avatar. |
| `group`          | boolean         | More than one other participant. |
| `memberCount`    | integer         | Number of other participants. |
| `preview`        | string          | One line, ≤ ~120 chars, of the latest message; prefixed `"You: "` when the user sent it. May be `""`. |
| `lastActivityAt` | string \| null  | Time of the latest message. |
| `unread`         | boolean         | Any thread in the conversation is unread. |
| `threadCount`    | integer         | Email threads merged into this conversation. |
| `accounts`       | string[]        | The user's own connected addresses this conversation came in on. Populated only when the list is unscoped (no effective `mailbox`); otherwise `[]`. |
| `senderEmail`    | string \| null  | Email of the latest message's sender (or the primary correspondent when the user sent it). |

Errors: `401 unauthorized`.

### `GET /api/v1/conversations/{key}`

One conversation's full timeline, oldest message first. **Read-only**: unlike
opening a conversation on the website, this does *not* mark it read — call
[`POST …/read`](#post-apiv1conversationskeyread).

| Query param | Type    | Notes |
|-------------|---------|-------|
| `mailbox`   | integer | Same meaning as on the list; restricts the timeline to threads involving that account. |

`200 OK`:

```json
{
  "key": "k-ada",
  "title": "Ada Lovelace",
  "initials": "AL",
  "group": false,
  "threadCount": 1,
  "members": [
    {
      "email": "ada@acme.com",
      "displayName": "Ada Lovelace",
      "initials": "AL",
      "label": "Ada Lovelace",
      "signatureText": "Ada · Acme"
    }
  ],
  "messages": [
    {
      "id": 1,
      "threadId": 11,
      "threadSubject": "Invoice",
      "sender": { "email": "ada@acme.com", "displayName": "Ada Lovelace", "initials": "AL" },
      "outbound": false,
      "sentAt": "2026-01-01T09:00:00Z",
      "bodyHtml": "<p>About the invoice.</p>",
      "bodyText": "About the invoice.",
      "replyToMessageId": null,
      "attachments": []
    }
  ]
}
```

#### ConversationDetail

| Field         | Type        | Notes |
|---------------|-------------|-------|
| `key`, `title`, `initials`, `group`, `threadCount` | | As in ConversationSummary. |
| `members`     | Member[]    | The other participants (never the user), first-seen order. |
| `messages`    | Message[]   | Oldest first. |

**Member:** `email` (string), `displayName` (string \| null), `initials`
(string), `label` (string — display name, else email), `signatureText` (string —
their most recent email signature as plain text, `""` if none detected).

**Message:**

| Field              | Type            | Notes |
|--------------------|-----------------|-------|
| `id`               | integer         | |
| `threadId`         | integer         | The email thread this message belongs to. |
| `threadSubject`    | string \| null  | That thread's subject. |
| `sender`           | object          | `email` (string), `displayName` (string \| null), `initials` (string). |
| `outbound`         | boolean         | `true` when the user sent it. |
| `sentAt`           | string \| null  | |
| `bodyHtml`         | string          | Server-sanitized HTML with quoted reply history and the signature stripped. |
| `bodyText`         | string          | The same content as plain text; paragraph and line breaks are `\n`. May be `""` (e.g. attachment-only mail). |
| `replyToMessageId` | integer \| null | `id` of the message this one replies to, when that message is in the same response. |
| `attachments`      | Attachment[]    | `id` (integer), `filename` (string \| null), `mimeType` (string \| null), `sizeBytes` (integer \| null). Metadata only — attachment bytes are not downloadable yet. |

**Bubble grouping rule** (the website applies it server-side; API clients apply
it to `messages`): walk the messages in order and start a new run whenever
`threadId` changes **or** `outbound` flips. The run's sender is its first
message's sender. The mobile client mirrors this in
`lib/domain/bubble_runs.dart`.

Errors: `401 unauthorized`, `404 not_found`.

### `POST /api/v1/conversations/{key}/read`

Marks every thread of the conversation read (within the `mailbox` scope, if
given — same query parameter as above). No body. Idempotent.

`204 No Content`. Errors: `401 unauthorized`, `404 not_found`.

---

## Not in the API yet

Sending/replying, composing a new conversation, live updates (the website's SSE
stream at `/chats/events`), mailbox management, registration, password reset,
Google sign-in, billing, saved searches, teams. These exist only as website
flows today; see `docs/WEB.md` and the mobile repo's `docs/DESIGN.md` §ROADMAP.
