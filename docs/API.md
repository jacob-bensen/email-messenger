# ConexusMail REST API — canonical contract

This file is the single source of truth for the JSON API the mobile client
(`conexus-mail-mobile`) talks to. The server implementation
(`com.emailmessenger.api`) and the client (`lib/data/` in the mobile repo)
must both match it exactly. **Change this file first**, then both
implementations, in the same run.

Status: **v1, partial.** Only the endpoints below exist. Everything else the
website can do (open a conversation, send, connect a mailbox, billing, …) has
no API yet — see "Not yet in the API" at the bottom.

## Conventions

| | |
|---|---|
| Base path | `/api/v1` |
| Format | JSON (`Content-Type: application/json`) request and response bodies, UTF-8 |
| Auth | `Authorization: Bearer <token>` on every endpoint except `POST /auth/login` |
| Timestamps | ISO-8601 UTC instants with a `Z` suffix, at most millisecond precision, e.g. `2026-01-02T09:30:15Z`. Nullable where noted. |
| IDs | JSON numbers (64-bit integers server-side) |
| Sessions | None. The API is stateless: no cookies are read or set, and no CSRF token is needed. A website session cookie does **not** authenticate `/api/**`, and a bearer token does **not** authenticate website pages. |

### Tokens

- Opaque strings beginning `cmx_`. Treat them as secrets; the server stores
  only a SHA-256 hash, so a token can't be recovered — only reissued by
  logging in again.
- Valid for 90 days from issue (`api.token.ttl-days`), until
  `POST /auth/logout`, or until the account's password is changed or reset
  (which revokes **all** of that user's tokens) — whichever comes first.
- A token for a disabled account is rejected.

### Errors

Every non-2xx response has this body:

```json
{ "error": { "code": "invalid_credentials", "message": "Invalid email or password." } }
```

`code` is stable and machine-readable; `message` is English, safe to show to
the user, and may change.

| HTTP | `code` | When |
|---|---|---|
| 400 | `invalid_request` | Body isn't valid JSON, a required field is missing/blank/too long, a query parameter has the wrong type, or the content type isn't JSON |
| 401 | `invalid_credentials` | `POST /auth/login` only: wrong email or password (see endpoint) |
| 401 | `unauthorized` | Any authenticated endpoint: token missing, malformed, expired, revoked, or its account is disabled. The client must discard the token and sign in again. |
| 404 | `not_found` | No such `/api/**` path, or wrong HTTP method for a path. (Unauthenticated callers get 401 before this.) |
| 429 | `too_many_attempts` | `POST /auth/login` only: login throttle is active |
| 500 | `internal_error` | Unexpected server failure |

## Shared objects

### `User`

```json
{
  "id": 42,
  "email": "ada@example.com",
  "displayName": "Ada Lovelace",
  "emailVerified": true,
  "plan": "PRO",
  "mailboxes": [ { "id": 7, "address": "ada@example.com" } ]
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | |
| `email` | string | Login email, lowercase |
| `displayName` | string \| null | |
| `emailVerified` | boolean | |
| `plan` | `"FREE"` \| `"PRO"` \| `"BUSINESS"` | Effective plan for entitlements, computed by the server: the subscription's tier while it is active or trialing, otherwise `FREE`. Clients must tolerate unknown values. |
| `mailboxes` | array | Connected mail accounts, oldest first. May be empty. |
| `mailboxes[].id` | number | Use as `mailboxId` on `GET /conversations` |
| `mailboxes[].address` | string | The account's address, lowercase |

## Endpoints

### `POST /api/v1/auth/login` — public

Exchange email + password for a bearer token.

Request:

```json
{ "email": "ada@example.com", "password": "…", "deviceName": "Pixel 9" }
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | yes | ≤ 254 chars. Trimmed and lowercased by the server. |
| `password` | string | yes | ≤ 200 chars. Used exactly as sent. |
| `deviceName` | string | no | ≤ 100 chars. Label stored with the token. |

`200`:

```json
{ "token": "cmx_…", "expiresAt": "2027-02-09T10:00:00Z", "user": { /* User */ } }
```

Errors: `400 invalid_request`, `401 invalid_credentials`,
`429 too_many_attempts`.

Rules (identical to the website's form login — they share one budget):

- `invalid_credentials` is returned, indistinguishably, for an unknown email,
  a wrong password, a disabled account, and an account that signed up with
  Google and has never set a password. (Such users set one via the website's
  "forgot password" flow.)
- Every failure counts toward the login throttle: 5 failures in 15 minutes
  for the same email **or** the same client IP (defaults; `auth.throttle.*`)
  locks further attempts — including ones with the correct password — and
  returns `429` until the window slides. Failures on the website and in the
  API count together.
- An unverified email does not block login (`user.emailVerified` reports it).

### `POST /api/v1/auth/logout` — bearer

Revokes the presented token. No body. `204` with an empty body. Idempotent
from the server's side, but a second call with the same token gets
`401 unauthorized` because the token no longer authenticates.

### `GET /api/v1/me` — bearer

`200`: a `User`. Use at launch to validate a stored token and refresh the
mailbox list.

### `GET /api/v1/conversations` — bearer

The chats list: one row per conversation (a person, or a group — the set of
people involved), most recently active first. This is the same data, from the
same service, as the website's `GET /chats`.

Query parameters (all optional):

| Param | Type | Default | Notes |
|---|---|---|---|
| `page` | integer | `0` | Zero-based. Negative values are treated as `0`. Non-numeric → `400`. A page past the end returns empty `items`. |
| `q` | string | — | Free-text search; trimmed, blank = no search. Case-insensitive substring match on thread subject, sender name/email, recipient name/email, and message plain-text body. |
| `filter` | string | `all` | `all` \| `unread` \| `awaiting` \| `attachments`, case-insensitive. An unrecognized value is treated as `all` (not an error). |
| `mailboxId` | integer | — | Scope to one connected mailbox (`User.mailboxes[].id`). Ignored — the list stays unscoped — if it isn't one of the user's mailboxes, isn't numeric, or the user has only one mailbox. |

`200`:

```json
{
  "items": [
    {
      "key": "3f9a…",
      "title": "Ada Lovelace, Bob Stone",
      "initials": "AL",
      "group": true,
      "memberCount": 2,
      "preview": "You: Kickoff Monday.",
      "lastActivityAt": "2026-01-02T09:30:15Z",
      "unread": true,
      "threadCount": 3,
      "accounts": ["me@example.com"],
      "senderEmail": "ada@acme.com"
    }
  ],
  "page": 0,
  "pageSize": 30,
  "totalItems": 57,
  "totalPages": 2,
  "hasNext": true,
  "counts": { "all": 57, "unread": 4, "awaiting": 9, "attachments": 12 }
}
```

| Field | Type | Notes |
|---|---|---|
| `items[].key` | string | Stable conversation id (`conversationKey`, derived server-side from the participant set). Opaque. |
| `items[].title` | string | Display label: the member's name/email, or a comma-joined list for a group |
| `items[].initials` | string | Avatar initials of the first member; `"?"` if there are no members |
| `items[].group` | boolean | `true` when there is more than one non-owner member |
| `items[].memberCount` | number | Non-owner members |
| `items[].preview` | string | Plain text of the latest message, whitespace-collapsed, ≤ 120 chars + `…`. Prefixed `You: ` when the user sent it. May be `""`. |
| `items[].lastActivityAt` | timestamp \| null | Time of the latest message |
| `items[].unread` | boolean | Any thread in the conversation is unread |
| `items[].threadCount` | number | Email threads merged into this conversation |
| `items[].accounts` | string[] | The user's own addresses this conversation involves. Populated only when the list is unscoped (no effective `mailboxId`); otherwise `[]`. |
| `items[].senderEmail` | string \| null | Latest message's sender if it was received; otherwise the first member's email |
| `page` | number | The page returned |
| `pageSize` | number | Fixed at 30; not client-configurable |
| `totalItems`, `totalPages` | number | For the current `q` + `filter` + scope |
| `hasNext` | boolean | Whether `page + 1` has rows |
| `counts.*` | number | Conversation totals per filter **for the current mailbox scope only** — they ignore `q` and `filter`, so they can label filter tabs. |

Side effect: like opening `/chats` on the website, this call asks the server
to refresh any of the user's mailboxes that are due for a poll, in the
background. It never waits for that; newly imported mail shows up on a later
call.

Because the list is ordered by recency, rows can shift between page fetches;
a client paging forward may see a conversation on two consecutive pages and
should de-duplicate by `key`, keeping the later copy.

Errors: `400 invalid_request`, `401 unauthorized`.

## Not yet in the API

The website can do all of this; the mobile client can't until endpoints are
specified here. Tracked in `INTERNAL_TODO.md` (backend) and
`docs/DESIGN.md` §ROADMAP (mobile).

- Open a conversation (message timeline, members, signatures) and mark it read
- Send a message / reply, with attachments; start a new conversation
- Live updates (the website uses SSE at `/chats/events`) and push notifications
- Register, verify email, reset password, Google sign-in
- Connect / reconnect / remove / sync a mailbox; signatures and send-from preference
- Saved searches, billing and plan changes, team invites, account settings
