# ConexusMail API contract

The canonical contract between the backend (`email-messenger`) and every
machine client — the mobile app (`conexus-mail-mobile`), the website's own
JavaScript, and Stripe. Both repos depend on this file. Change it **before**
changing either implementation, and change both implementations in the same
run.

Status legend: **Implemented** = exists in code and is tested.
**Planned** = agreed direction, no code yet; shapes are not final until the
section is promoted to Implemented.

## 1. Current state (2026-11-17)

`/api/v1` exposes bearer-token sign-in, sign-out, the current user, the
paged conversation list, one conversation's bubble timeline and mark read
(§5). Sending and mailboxes are not exposed yet (§6). The website's own machine endpoints (§3) still use the
browser session cookie and are not for mobile use.

## 2. Authentication

Two independent Spring Security chains:

| Paths | Chain | Credential | CSRF | Unauthenticated |
|---|---|---|---|---|
| `/api/**` | `ApiSecurityConfig` (order 1, stateless, never creates a session) | `Authorization: Bearer <token>` | off | `401` + JSON error envelope (§5.1) |
| everything else | `SecurityConfig` (website) | session cookie `JSESSIONID` | on | `302 → /login` |

Website: form login `POST /login` (`email`, `password`, optional
`remember-me`) or Google OIDC. Every non-GET request needs a CSRF token
(form field `_csrf`, or header `X-CSRF-TOKEN`), except
`POST /billing/webhook`. A `fetch`/SSE caller without a session also gets
the `302`, never `401`.

Both chains share one login throttle: 5 failures within 15 minutes for the
same email **or** the same client IP locks further attempts (website:
redirect `/login?error=locked`; API: `429 too_many_attempts`). Failures and
successes from either path land in the same `auth_events` audit log and
stamp `users.last_login_at`.

API tokens are opaque (43-char base64url of 32 random bytes), valid for
**30 days** from issue, one per signed-in device. Only SHA-256(token) is
stored (`api_tokens`). A token stops working when it expires, when it is
revoked by `POST /api/v1/auth/logout`, or when the user is disabled. There
is no refresh endpoint: when a request returns `401`, the client signs in
again. Google sign-in on mobile is not supported yet. No CORS is configured
(native clients do not need it).

## 3. Implemented machine endpoints

### POST /chats/refresh
Synchronously polls the signed-in user's mailboxes that have not synced in
the last minute. Used by the website's 60-second heartbeat.

- Auth: session cookie + CSRF header.
- Request body: none.
- `200 application/json`:
  ```json
  { "imported": 3 }
  ```
  `imported` (integer ≥ 0): messages imported across the user's mailboxes
  (received + sent). Per-mailbox IMAP failures are logged and count as 0.

### GET /chats/events
Server-Sent Events stream of new-mail notifications for the signed-in user.

- Auth: session cookie.
- `200 text/event-stream`. The server closes the stream after 30 minutes;
  clients reconnect.
- Event `new-mail`, data is JSON:
  ```
  event: new-mail
  data: {"imported":2}
  ```
  Sent after a poll commits with at least one imported message. It carries a
  count only — no conversation keys or message ids — so the client refetches.
- Comment line `:ping` every 25 s (`mailbox.sse.keepalive-ms`).
- No event ids, no replay of missed events. Fan-out is in-memory, so it
  works on a single backend instance only.

### GET /chats/{key}/pane
HTML fragment (not JSON) of one conversation, used by the website to swap
the open pane in place. Marks the conversation read. Returns `200` with a
"missing" fragment when the key is unknown. Not for mobile use.

### POST /billing/webhook
Stripe webhook receiver.

- Auth: `Stripe-Signature` header verified against
  `billing.stripe.webhook-secret`. Public, CSRF-exempt.
- Request: raw Stripe event JSON.
- Responses (`text/plain`): `200 ok`; `400 missing Stripe-Signature header`;
  `400 invalid signature`; `503 webhook not configured`; `500 error`.
- Handled event types: `checkout.session.completed`,
  `customer.subscription.created`, `customer.subscription.updated`,
  `customer.subscription.deleted`. Anything else is acknowledged with `200`.

### GET /manifest.webmanifest, GET /sw.js
PWA manifest (`application/manifest+json`: `name`, `short_name`,
`description`, `start_url` `/chats`, `scope` `/`, `display` `standalone`,
`orientation`, `lang`, `dir`, `theme_color` `#2f855a`, `background_color`
`#0f1512`, `icons[]`) and service worker. Public.

### GET /actuator/health
Spring Boot health, public: `{"status":"UP"}`.

### Errors (website endpoints only)
Every other failure on these endpoints is rendered as an **HTML** error page
(`error` template) with the status from `GlobalExceptionHandler`: 400
(`IllegalArgumentException`), 404 (unknown/not-owned resource), 409 (data
integrity), 413 (upload too large: 10 MB/file, 25 MB/request), 502
(mail or billing provider failure), 500. There is no JSON error envelope yet.

## 4. Shared rules (Implemented on both sides)

Rules the server owns and the client mirrors in `lib/domain/`. The server's
answer always wins.

### Conversation key
A conversation is all of one owner's threads that share a key; the key
identifies the set of non-owner people. Server: `ConversationKeyService`.
Client: `lib/domain/conversation_key.dart`.

1. Take every message's sender plus To/Cc recipients (Bcc excluded).
2. Trim (Java `String.trim()` semantics) and lowercase each address; drop
   blanks and the owner's own addresses (account email + every connected
   mailbox address, normalized the same way).
3. De-duplicate, sort ascending by UTF-16 code unit, join with `\n`. If
   nothing is left, use the sentinel `U+0000` followed by `self`.
4. SHA-256 the UTF-8 bytes; the key is the first **32 lowercase hex chars**.

Golden vectors (owner addresses = `me@example.com`), asserted by
`ConversationKeyServiceTest#goldenVectorsMatchTheMobileClientMirror` and
`test/domain/conversation_key_test.dart`:

| Emails | Key |
|---|---|
| `ada@acme.com` | `154e1e0a1872533ddf397a16e5a65dcf` |
| ` Bob@Acme.com`, `ada@acme.com`, `ME@example.com`, `ADA@ACME.COM ` | `487b97fee6493a379888b62e3969cb14` |
| `me@example.com`, blank | `95270985d1d4b88724c13887a7794ec3` |

### Conversation title and initials
Server: `ConversationLabels`. Client: `lib/domain/conversation_labels.dart`.
Title: `You` (no members), the single label, or the first three labels joined
with `, ` plus ` +N` for the rest. Initials: first letters of the first two
whitespace-separated words of the display name, uppercased; with no display
name, the first letter of the email local-part; otherwise `?`.

### Plan limits
Server: `PlanLimits` / `PlanLimitService`. Client:
`lib/domain/plan_limits.dart`. A plan counts only while the subscription
status is `active` or `trialing`; otherwise the user is FREE.

| Plan | Mailboxes | Threads | Saved searches |
|---|---|---|---|
| FREE | 3 | 500 | unlimited |
| PRO | 5 | unlimited | unlimited |
| BUSINESS | unlimited | unlimited | unlimited |

## 5. `/api/v1` (Implemented)

Base path `/api/v1`. Request and response bodies are `application/json`,
UTF-8, field names in camelCase. Timestamps are ISO-8601 UTC instants with a
`Z` suffix (e.g. `2026-12-16T04:20:47.566Z`). Server code:
`com.emailmessenger.api`. Client code: `lib/data/`.

### 5.1 Error envelope
Every `/api/**` error, including authentication failures from the security
chain, is:
```json
{ "error": { "code": "invalid_credentials", "message": "Incorrect email or password." } }
```
`code` is stable and machine-readable; `message` is English, safe to show to
the user, and may change. Clients branch on `code` only.

| HTTP | `code` | When |
|---|---|---|
| 400 | `validation_failed` | Body missing, not JSON, a required field blank, or a query parameter out of range |
| 404 | `not_found` | A referenced resource (e.g. `mailboxId`) is not the user's |
| 401 | `unauthorized` | No/invalid/expired/revoked bearer token, or user disabled |
| 401 | `invalid_credentials` | Login: wrong email or password (same for unknown email) |
| 403 | `forbidden` | Authenticated but not allowed |
| 404 | `not_found` | Unknown `/api` path (after authentication) |
| 405 | `method_not_allowed` | Wrong HTTP method on an existing endpoint |
| 429 | `too_many_attempts` | Login: throttle lock (§2) |
| 500 | `internal_error` | Anything unexpected |

Clients must treat an unknown `code` like `internal_error` for its status.

### 5.2 POST /api/v1/auth/login
Public. Exchanges email + password for a bearer token.

Request:
```json
{ "email": "ada@example.com", "password": "correct-horse", "deviceName": "Pixel 9" }
```
- `email` (string, required, non-blank): case- and surrounding-whitespace-
  insensitive.
- `password` (string, required, non-blank).
- `deviceName` (string, optional, ≤ 100 chars; longer is a
  `validation_failed`): shown to the user in a future device list. Blank is
  stored as null.

`200`:
```json
{
  "token": "q1Zr…43 chars…",
  "tokenType": "Bearer",
  "expiresAt": "2026-12-16T04:20:47.566Z",
  "user": { …same object as GET /api/v1/me… }
}
```
Errors: `400 validation_failed`, `401 invalid_credentials`,
`429 too_many_attempts`. A Google-only account (no password set) gets
`401 invalid_credentials`. Unverified email addresses may sign in, as on the
website. No `Set-Cookie` is ever sent.

### 5.3 POST /api/v1/auth/logout
Bearer token required. Revokes **only the token used for this request**;
other devices stay signed in. Request body: none. `204`, empty body.
Errors: `401 unauthorized`.

### 5.4 GET /api/v1/me
Bearer token required. The signed-in user.

`200`:
```json
{
  "id": 42,
  "email": "ada@example.com",
  "displayName": "Ada Lovelace",
  "emailVerified": false,
  "plan": "free",
  "limits": { "mailboxes": 3, "threads": 500, "savedSearches": null }
}
```
- `id` (integer), `email` (string, normalized lowercase).
- `displayName` (string or `null`).
- `emailVerified` (boolean).
- `plan` (string): `free`, `pro` or `business` — the **effective** plan
  (§4 Plan limits: a lapsed subscription is `free`).
- `limits` (object): caps of the effective plan per §4; each value is an
  integer or `null` meaning unlimited.

Errors: `401 unauthorized`.

### 5.5 GET /api/v1/conversations
Bearer token required. One page of the user's conversations (one row per
person or group, §4 Conversation key), most recently active first, plus
the per-filter counts for the same mailbox scope. The same
`ConversationListService` query the website's `/chats` list uses; like the
website, the request also kicks off a non-blocking refresh of mailboxes
that are due for a sync (results show up on a later request).

Query parameters (all optional):

| Name | Type | Default | Rule |
|---|---|---|---|
| `page` | integer ≥ 0 | `0` | Zero-based page index |
| `size` | integer 1–100 | `30` | Page size |
| `q` | string | — | Free-text search; blank = none. Matches subject, body or a participant's name/address |
| `mailboxId` | integer | — | Scope to one connected mailbox (an id the user owns). Omitted = all mailboxes merged |
| `filter` | `all`, `unread`, `awaiting`, `attachments` (case-insensitive) | `all` | `unread`: has an unread thread. `awaiting`: latest message is inbound (you owe a reply). `attachments`: has an attachment |

Scope rule (server-owned, same as the website): a `mailboxId` narrows the
list only when the user has **more than one** connected mailbox; with a
single mailbox every conversation belongs to it, so the list is unscoped.

`200`:
```json
{
  "items": [
    {
      "key": "154e1e0a1872533ddf397a16e5a65dcf",
      "title": "Ada Lovelace",
      "initials": "AL",
      "group": false,
      "memberCount": 1,
      "preview": "You: sounds good, see you then",
      "lastActivityAt": "2026-11-16T09:30:00Z",
      "unread": true,
      "threadCount": 2,
      "accounts": ["work@acme.com"],
      "senderEmail": "ada@acme.com"
    }
  ],
  "page": 0,
  "size": 30,
  "totalItems": 1,
  "hasNext": false,
  "counts": { "all": 1, "unread": 1, "awaiting": 0, "attachments": 0 }
}
```
- `key` (string): the conversation key (§4); stable id for later endpoints.
- `title`, `initials` (string): §4 Conversation title and initials.
- `group` (boolean): more than one non-owner member; `memberCount` (integer).
- `preview` (string): latest message as one line of plain text, ≤ 120
  chars plus `…`; prefixed `You: ` when you sent it; `""` when empty.
- `lastActivityAt` (instant or `null`): when the latest message was sent.
- `unread` (boolean): any thread in it is unread.
- `threadCount` (integer ≥ 1): email threads merged into this conversation.
- `accounts` (string[]): the user's own addresses this conversation came in
  on. Filled only for the unscoped list; `[]` when scoped.
- `senderEmail` (string or `null`): latest inbound sender, else the first
  member's address.
- `totalItems` (integer): rows matching `q` + `filter` in scope.
- `counts` (object of integers): conversations in scope per filter,
  ignoring `q` and `filter`.

Errors: `400 validation_failed` (`page` < 0, `size` outside 1–100, a
non-integer number, or an unknown `filter`); `404 not_found` (`mailboxId`
is not one of the user's mailboxes); `401 unauthorized`.

### 5.6 GET /api/v1/conversations/{key}
Bearer token required. One conversation as the website's `/chats/{key}`
timeline shows it: every message with these people, merged across email
threads, oldest first, with quoted reply history and signatures stripped,
grouped into bubble runs. Built by the same `ChatService` as the website.
**Read-only** — unlike opening a chat on the website, it does not mark the
conversation read; call §5.7 for that. All messages are returned (no
pagination yet).

Path: `key` — a conversation key from §5.5 (§4 Conversation key).
Query: `mailboxId` (integer, optional) — same meaning and scope rule as
§5.5: with more than one mailbox it limits the timeline to threads that
involve that mailbox's address.

`200`:
```json
{
  "key": "154e1e0a1872533ddf397a16e5a65dcf",
  "title": "Ada Lovelace",
  "initials": "AL",
  "group": false,
  "threadCount": 1,
  "messageCount": 2,
  "members": [
    { "email": "ada@acme.com", "displayName": "Ada Lovelace", "initials": "AL", "label": "Ada Lovelace" }
  ],
  "runs": [
    {
      "sender": { "email": "ada@acme.com", "displayName": "Ada Lovelace", "initials": "AL" },
      "outbound": false,
      "thread": { "id": 17, "subject": "Lunch" },
      "messages": [
        {
          "id": 301,
          "sentAt": "2026-11-16T09:00:00Z",
          "bodyHtml": "<p>Pizza?</p>",
          "bodyText": "Pizza?",
          "replyToId": null,
          "attachments": [ { "filename": "menu.pdf", "mimeType": "application/pdf", "sizeBytes": 1234 } ]
        }
      ]
    },
    {
      "sender": { "email": "me@example.com", "displayName": null, "initials": "M" },
      "outbound": true,
      "thread": { "id": 17, "subject": "Lunch" },
      "messages": [
        { "id": 305, "sentAt": "2026-11-16T09:05:00Z", "bodyHtml": "<p>yes!</p>",
          "bodyText": "yes!", "replyToId": 301, "attachments": [] }
      ]
    }
  ]
}
```
- `key`, `title`, `initials`, `group`: as in §5.5.
- `threadCount` (integer ≥ 1), `messageCount` (integer ≥ 1).
- `members` (array): the non-owner people, first-seen order. `email`;
  `displayName` (string or `null`); `initials`; `label` (display name, else
  the email) — the strings §4 titles are built from.
- `runs` (array, chronological): consecutive messages from the same side of
  the same thread. A new run starts whenever the thread or the direction
  changes. `outbound` is `true` for the user's own messages (render as
  "you", right-aligned). `sender` is the run's first sender (`displayName`
  string or `null`). `thread.id` (integer) and `thread.subject` (string or
  `null`) badge the run.
- `messages[].id` (integer): message id. `sentAt` (instant or `null`).
- `bodyHtml` (string): the sanitized HTML fragment the website renders
  (quotes/signature removed); `""` when empty. `bodyText` (string): the
  same as plain text with line (`\n`) and paragraph (`\n\n`) breaks kept,
  trimmed; for clients that do not render HTML.
- `replyToId` (integer or `null`): id of the message this one replies to,
  when that message is in this same response.
- `attachments` (array): `filename` (string), `mimeType` (string or
  `null`), `sizeBytes` (integer or `null`). Metadata only — the server does
  not store attachment bytes yet, so there is no download.

Errors: `404 not_found` (no conversation with that key for this user in
that scope — including another user's key — or `mailboxId` not the
user's); `401 unauthorized`; `400 validation_failed` (non-integer
`mailboxId`).

### 5.7 POST /api/v1/conversations/{key}/read
Bearer token required. Marks every thread of the conversation read (within
the `mailboxId` scope, same rule as §5.6), exactly what opening a chat on
the website does. Idempotent. Request body: none.

`200`:
```json
{ "counts": { "all": 12, "unread": 3, "awaiting": 5, "attachments": 1 } }
```
`counts`: the §5.5 per-filter counts for the same scope, after the
change, so the client can update its filter chips without refetching.

Errors: `404 not_found` (as §5.6; nothing is changed); `401 unauthorized`;
`400 validation_failed`.

## 6. Planned (not implemented)

Intended resources, in build order (see the mobile repo's
`docs/DESIGN.md` §ROADMAP):
1. Send: reply into a conversation, start a new one.
2. Mailboxes: list, connect (IMAP password), sync, remove.
3. Account: device list / revoke other sessions.
4. Live updates: SSE (or push) with enough detail to update without a
   full refetch.
