# ConexusMail REST API — canonical contract

This file is the single source of truth for the JSON API consumed by the
mobile client (`conexus-mail-mobile`). The server implementation
(`com.emailmessenger.api`) and the client's networking layer
(`lib/data/`) must both match it exactly. Change this file **first**, then
both implementations in the same run.

The contract is pinned by `MobileApiIntegrationTest` (backend) and
`test/data/api_client_test.dart` + `test/fixtures.dart` (mobile).

## Conventions

| Topic | Rule |
| --- | --- |
| Base path | `/api/v1` |
| Format | JSON, UTF-8. Requests with a body send `Content-Type: application/json`. |
| Auth | `Authorization: Bearer <token>` on every endpoint except `POST /auth/login`. Cookies/sessions are ignored on `/api/**`; there is no CSRF token. |
| Timestamps | ISO-8601 UTC instants, e.g. `"2026-01-01T09:00:00Z"`. May be `null` where noted. |
| Ids | JSON numbers (64-bit). Conversation `key` is an opaque string computed by the server from the participant set. |
| Unknown fields | Clients must ignore fields they don't know. Servers ignore unknown request fields. |
| Ownership | Every resource is scoped to the token's user. Another user's resource is indistinguishable from a missing one (`404`). |

### Error envelope

Every non-2xx response from an `/api/v1` controller has this body:

```json
{ "error": { "code": "invalid_credentials", "message": "Incorrect email or password." } }
```

`code` is stable and machine-readable; `message` is human-readable English
and may change.

| HTTP | `code` | When |
| --- | --- | --- |
| 400 | `validation_failed` | A request field failed validation. |
| 400 | `bad_request` | Malformed JSON, wrong parameter type, or an unknown enum value (e.g. `filter`). |
| 401 | `invalid_credentials` | Login only: wrong email/password, disabled account, or an account with no password (Google-only). Deliberately identical for all of these. |
| 401 | `unauthorized` | Missing, malformed, expired, or revoked bearer token. The client must discard its token. |
| 404 | `not_found` | The resource doesn't exist for this user. |
| 429 | `too_many_attempts` | Login only: the email or client IP is locked out (5 failures in 15 minutes by default, shared with the website login). |
| 500 | `internal_error` | Unhandled server error. |

Caveat: a request to an `/api/**` path that matches **no** controller is
answered by the website's error handling (HTML `404`), not this envelope.
Clients must tolerate non-JSON error bodies.

## Shared schemas

### `User`

| Field | Type | Notes |
| --- | --- | --- |
| `id` | number | |
| `email` | string | Normalized (trimmed, lower-case) login email. |
| `displayName` | string \| null | |
| `emailVerified` | boolean | |
| `plan` | `"FREE"` \| `"PRO"` \| `"BUSINESS"` | Effective plan (an active/trialing paid subscription, else `FREE`). Clients must treat unknown values as `FREE`. |

## Endpoints

### `POST /api/v1/auth/login`

Auth: none. Exchanges a password for a bearer token. Counts toward (and is
blocked by) the same failure throttle as the website login form, and writes
the same `auth_events` audit rows.

Request:

| Field | Type | Rules |
| --- | --- | --- |
| `email` | string | Required, non-blank, ≤ 254 chars. Trimmed and lower-cased by the server. |
| `password` | string | Required, non-blank, ≤ 200 chars. |

Response `200`:

```json
{
  "token": "pA3…43 URL-safe base64 chars…",
  "expiresAt": "2027-02-09T12:00:00Z",
  "user": { "id": 7, "email": "owner@example.com", "displayName": "Owner", "emailVerified": true, "plan": "PRO" }
}
```

The token is opaque, valid for 90 days, and shown only once (the server
stores a SHA-256 hash). Each login issues a new token; existing tokens stay
valid.

Errors: `400 validation_failed`, `400 bad_request`, `401 invalid_credentials`,
`429 too_many_attempts`.

### `POST /api/v1/auth/logout`

Auth: bearer. Revokes the presented token. Response `204`, no body.
Errors: `401 unauthorized`.

### `GET /api/v1/me`

Auth: bearer. Response `200`: a `User`. Errors: `401 unauthorized`.

### `GET /api/v1/conversations`

Auth: bearer. The chats list across all of the user's connected mailboxes
(the website's cross-account Dashboard view), most recently active first.

Query parameters:

| Param | Type | Default | Notes |
| --- | --- | --- | --- |
| `page` | integer | `0` | Zero-based. Negative values are treated as `0`. |
| `size` | integer | `30` | Clamped to `1..100`. |
| `q` | string | — | Optional free-text search; blank is ignored. |
| `filter` | `all` \| `unread` \| `awaiting` \| `attachments` | `all` | Case-insensitive. Unknown value → `400 bad_request`. |

Response `200`:

```json
{
  "items": [
    {
      "key": "c0ffee…",
      "title": "Ada Lovelace",
      "initials": "AL",
      "group": false,
      "memberCount": 1,
      "preview": "First message",
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
  "hasNext": false,
  "counts": { "all": 1, "unread": 1, "awaiting": 0, "attachments": 1 }
}
```

`ConversationSummary` fields:

| Field | Type | Notes |
| --- | --- | --- |
| `key` | string | Conversation key; use in the detail/read paths. |
| `title` | string | Server-built label from the non-owner members. |
| `initials` | string | Avatar initials of the first member (`"?"` if none). |
| `group` | boolean | More than one non-owner member. |
| `memberCount` | number | Non-owner members. |
| `preview` | string | Plain-text one-liner of the latest message, ≤ 120 chars + `…`; prefixed `"You: "` when the latest message is outbound. May be `""`. |
| `lastActivityAt` | timestamp \| null | |
| `unread` | boolean | Any thread in the conversation is unread. |
| `threadCount` | number | Email threads merged into this conversation. |
| `accounts` | string[] | The user's own addresses this conversation came in on. May be empty. |
| `senderEmail` | string \| null | Latest inbound sender, else the primary correspondent. |

`counts` are conversation totals per filter for the whole list (independent
of `q`, `filter`, and paging).

Errors: `400 bad_request`, `401 unauthorized`.

### `GET /api/v1/conversations/{key}`

Auth: bearer. One conversation's merged timeline, oldest first. **Does not
mark the conversation read** — call the `read` endpoint.

Response `200`:

```json
{
  "key": "c0ffee…",
  "title": "Ada Lovelace",
  "initials": "AL",
  "group": false,
  "threadCount": 1,
  "messageCount": 1,
  "members": [
    { "email": "ada@acme.com", "displayName": "Ada Lovelace", "initials": "AL", "label": "Ada Lovelace", "signatureHtml": "" }
  ],
  "runs": [
    {
      "sender": { "email": "ada@acme.com", "displayName": "Ada Lovelace", "initials": "AL" },
      "thread": { "id": 3, "subject": "Project Athena" },
      "outbound": false,
      "messages": [
        {
          "id": 11,
          "bodyHtml": "<p>First message</p>",
          "sentAt": "2026-01-01T09:00:00Z",
          "outbound": false,
          "replyToMessageId": null,
          "attachments": [
            { "id": 5, "filename": "spec.pdf", "mimeType": "application/pdf", "sizeBytes": 1234 }
          ]
        }
      ]
    }
  ]
}
```

| Object | Field | Type | Notes |
| --- | --- | --- | --- |
| detail | `members` | `Member[]` | Non-owner participants. |
| detail | `runs` | `Run[]` | Chronological. A run is consecutive messages from one side within one email thread; the server does the grouping. |
| `Member` | `displayName` | string \| null | |
| `Member` | `label` | string | Display name, else email. |
| `Member` | `signatureHtml` | string | Server-sanitized HTML of the member's latest detected signature; `""` if none. |
| `Run` | `sender` | object \| null | `email`, `displayName` (nullable), `initials`. |
| `Run` | `thread` | object \| null | `id`, `subject` (nullable). |
| `Run` | `outbound` | boolean | `true` = sent by the user ("you" bubbles). |
| `Message` | `bodyHtml` | string | Server-sanitized HTML with quoted replies and signatures stripped. Never raw email HTML. |
| `Message` | `sentAt` | timestamp \| null | |
| `Message` | `replyToMessageId` | number \| null | Id of the message this replies to, when that message is in the same conversation. |
| `Attachment` | `mimeType`, `sizeBytes` | string \| null, number \| null | Metadata only — attachment bytes are not downloadable yet. |

Errors: `401 unauthorized`, `404 not_found`.

### `POST /api/v1/conversations/{key}/read`

Auth: bearer. Marks every thread of the conversation read. Idempotent; an
unknown key is a no-op. No request body. Response `204`, no body.
Errors: `401 unauthorized`.

## Not in the API yet

Tracked in `INTERNAL_TODO.md`: sending/replying, composing a new
conversation, per-mailbox scoping (`mailbox` parameter), live updates
(the website's SSE stream `/chats/events`), triggering a mailbox refresh,
registration, Google sign-in, attachment download, mailbox management,
billing, saved searches, teams.
