# ConexusMail API contract

The canonical contract between the backend (`email-messenger`) and every
machine client — the mobile app (`conexus-mail-mobile`), the website's own
JavaScript, and Stripe. Both repos depend on this file. Change it **before**
changing either implementation, and change both implementations in the same
run.

Status legend: **Implemented** = exists in code and is tested.
**Planned** = agreed direction, no code yet; shapes are not final until the
section is promoted to Implemented.

## 1. Current state (2026-11-19)

`/api/v1` exposes bearer-token sign-in, sign-out, the current user, the
paged conversation list, one conversation's timeline, mark-read, sending
into a conversation, starting a new one, mailboxes (list, connect
with an IMAP password, sync now, remove), the account: plan summary,
signed-in devices, sign a device out, and a live event stream of
conversation-list changes (§5). Gmail OAuth connect,
signatures and mail preferences are website-only. The website's own machine endpoints (§3) still use the
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
| 403 | `plan_limit_reached` | Connect mailbox: already at the plan's mailbox cap (§5.12) |
| 404 | `not_found` | Unknown `/api` path (after authentication) |
| 405 | `method_not_allowed` | Wrong HTTP method on an existing endpoint |
| 409 | `no_recipients` | Send: the conversation has nobody but you in it (§5.8) |
| 422 | `mailbox_connect_failed` | Connect mailbox: IMAP login or connection failed (§5.12) |
| 429 | `too_many_attempts` | Login: throttle lock (§2) |
| 500 | `internal_error` | Anything unexpected |
| 502 | `send_failed` | Send: the mail server refused or could not be reached; nothing was sent or recorded (§5.8, §5.9) |

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
Bearer token required. One conversation as a texting-style timeline: every
message of the user's threads that share `key` (§4), merged across email
threads, oldest first, grouped into bubble runs. The same `ChatService`
build the website's `/chats/{key}` uses. Reading does **not** mark the
conversation read (§5.7) and does not trigger a mailbox refresh.

Path: `key` — a conversation key from §5.5. Anything that is not one of the
user's conversations (including a malformed key) is `404 not_found`.

Query parameter (optional): `mailboxId` (integer) — same meaning and scope
rule as §5.5: with more than one connected mailbox, only the threads that
involve that mailbox's address are included.

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
      "outbound": false,
      "sender": { "email": "ada@acme.com", "displayName": "Ada Lovelace", "initials": "AL" },
      "thread": { "id": 7, "subject": "Lunch" },
      "messages": [
        {
          "id": 101,
          "sentAt": "2026-11-16T09:30:00Z",
          "bodyHtml": "<p>Pizza at noon?</p>",
          "bodyText": "Pizza at noon?",
          "replyToId": null,
          "attachments": [ { "filename": "menu.pdf", "mimeType": "application/pdf", "sizeBytes": 20480 } ]
        }
      ]
    }
  ]
}
```
- `key`, `title`, `initials`, `group`: as in §5.5. `threadCount`
  (integer ≥ 1), `messageCount` (integer ≥ 1, total messages over all runs).
- `members` (array): the non-owner participants in first-seen order.
  `displayName` is a string or `null`; `label` is the display name, else the
  email (§4 title input); `initials` per §4.
- `runs` (array, chronological): consecutive messages from the same side
  within the same email thread. `outbound` (boolean): sent by the user.
  `sender` (object): who sent the run's messages (`displayName` may be
  `null`). `thread` (object): the email thread the run belongs to (`id`
  integer, `subject` string or `null`).
- `messages[]`, oldest first within the run:
  - `id` (integer): message id.
  - `sentAt` (instant or `null`).
  - `bodyHtml` (string): the server-sanitized body with quoted replies and
    the signature removed (jsoup `Safelist.relaxed()`: text formatting,
    lists, tables, links and `img`; no scripts, styles or event
    attributes). May be `""`.
  - `bodyText` (string): the same body as plain text — block elements and
    `<br>` become line breaks, runs of blank lines collapse to one, trimmed.
    Clients that do not render HTML show this. May be `""`.
  - `replyToId` (integer or `null`): the message this one answers, when
    that message is also in this timeline.
  - `attachments` (array): `filename` (string), `mimeType` (string or
    `null`), `sizeBytes` (integer or `null`). Metadata only — attachment
    bytes are not stored yet, so there is no download endpoint.

Signatures (shown in the website's side panel) are not part of the
response yet. There is no pagination: the whole conversation is returned.

Errors: `404 not_found` (unknown key, or `mailboxId` not the user's);
`400 validation_failed` (non-integer `mailboxId`); `401 unauthorized`.

### 5.7 POST /api/v1/conversations/{key}/read
Bearer token required. Marks every thread of the conversation read — only
the threads involving `mailboxId`'s address when that query parameter
scopes it (§5.5 scope rule), so the same person reached through another
mailbox keeps its unread state. The same `ChatService.markRead` the website
runs when a conversation is opened. Idempotent. Request body: none.

`204`, empty body. Errors: `404 not_found` (no such conversation in scope,
or `mailboxId` not the user's); `401 unauthorized`.

Clients may show the row as read immediately (prediction) and must take the
next `GET /api/v1/conversations` as the truth.

### 5.8 POST /api/v1/conversations/{key}/messages
Bearer token required. Sends a message to everyone in the conversation and
records it as your bubble. The same `ChatSendService` the website's
reply box (`POST /chats/{key}/send`) uses, so recipients, sending account,
signature, subject and threading follow one rule set.

Path: `key` — a conversation key (§5.5). Query parameter (optional):
`mailboxId` — scopes the conversation lookup exactly like §5.6, **and**
picks the sending account.

Request:
```json
{ "body": "Pizza at noon works.", "subject": null, "asReply": true }
```
- `body` (string, required): the message text, plain text. Must not be
  blank; at most 100 000 characters. Attachments cannot be sent through the
  API yet.
- `subject` (string, optional): subject of the new email when `asReply` is
  false. Blank or missing → `(no subject)`. Ignored when `asReply` is true.
- `asReply` (boolean, optional, default `false`): `true` continues the
  conversation's most recently active email thread (subject
  `Re: <thread subject>`, `In-Reply-To`/`References` set to that thread's
  latest message); `false` sends a new email that the server files under
  this same conversation.

Server rules (clients must not predict beyond the bubble text):
- Recipients are the conversation's members (§5.6 `members`), reply-all.
- Sending account: `mailboxId` if given; else the user's default sending
  mailbox if it still exists; else the only connected mailbox; else the
  account email. The account's signature, if set, is appended under a
  `-- ` line, so the recorded bubble matches what recipients got.
- The mail is sent first and recorded only after the mail server accepted
  it. The recorded message is outbound and its thread is marked read.

`201`:
```json
{ "key": "154e1e0a1872533ddf397a16e5a65dcf", "threadId": 7, "messageId": 102 }
```
- `key` (string): the conversation the message was filed under (equals
  the path key). `threadId`, `messageId` (integer): the recorded message.
  Clients refetch §5.6 to show it; the server's timeline replaces any
  locally predicted bubble.

Errors: `400 validation_failed` (body blank or too long, malformed JSON,
non-integer `mailboxId`); `404 not_found` (unknown key in scope, or
`mailboxId` not the user's); `409 no_recipients`; `502 send_failed`;
`401 unauthorized`. The request is not idempotent: a retry after a
timeout may send twice.

### 5.9 POST /api/v1/conversations
Bearer token required. Starts a new conversation — or adds to the existing
one with the same people — by sending a new email. The same
`ChatSendService` path as the website's `POST /chats/new`.

Query parameter (optional): `mailboxId` — the sending account (rule as
§5.8; `404 not_found` if not the user's).

Request:
```json
{ "to": ["bob@acme.com", "cy@acme.com"], "subject": "Lunch", "body": "Pizza at noon?" }
```
- `to` (string[], required): recipients. Each entry is split on commas,
  semicolons and whitespace (as the website's To field), parts without `@`
  are dropped, and duplicates are removed. Nothing left →
  `400 validation_failed`.
- `subject` (string, optional): blank or missing → `(no subject)`.
- `body` (string, required): as §5.8.

`201`: same shape as §5.8. `key` is computed by the server from the
recipients (§4 Conversation key; your own addresses are excluded) — the
conversation to open next. Errors: `400 validation_failed`,
`404 not_found` (`mailboxId`), `502 send_failed`, `401 unauthorized`.

### 5.10 GET /api/v1/mailboxes
Bearer token required. The user's connected mailboxes, oldest first (the
same `MailAccountService.list` as the website's `/mailboxes`), plus the
plan's mailbox cap.

`200`:
```json
{
  "items": [
    {
      "id": 3,
      "address": "ada@example.com",
      "host": "imap.gmail.com",
      "port": 993,
      "ssl": true,
      "authType": "password",
      "isDefault": true,
      "lastSyncedAt": "2026-11-19T08:15:00Z",
      "lastSyncError": null,
      "errorHint": null,
      "pollingSuspended": false
    }
  ],
  "limit": 3
}
```
Mailbox object (also returned by §5.12 and §5.13):
- `id` (integer) — the `mailboxId` that §5.5–5.9 accept.
- `address` (string): the IMAP login, normally the email address.
- `host` (string), `port` (integer), `ssl` (boolean): IMAP endpoint.
- `authType` (string): `password` (IMAP password / app password) or
  `google` (Gmail OAuth, connected on the website). Unknown future values
  must be shown as a generic mailbox, not rejected.
- `isDefault` (boolean): the user's default sending mailbox (website
  setting). At most one is `true`; none when no default is set.
- `lastSyncedAt` (instant or `null` = never synced).
- `lastSyncError` (string or `null`): the last poll's error, raw. Cleared
  by the next successful poll.
- `errorHint` (string or `null`): what to do about `lastSyncError`, in
  plain English (`MailboxView.errorHint`, the same text the website shows).
  `null` exactly when `lastSyncError` is `null`.
- `pollingSuspended` (boolean): automatic polling stopped after repeated
  failures; a successful Sync now (§5.13) or reconnect resumes it.

`limit` (integer or `null` = unlimited): the effective plan's mailbox cap
(same as `GET /me` `limits.mailboxes`). Errors: `401 unauthorized`.

### 5.11 GET /api/v1/mailboxes/providers
Bearer token required. Presets for the connect form, in display order (the
website's connect wizard, `MailboxProvider`). The server owns the list;
clients must not hard-code hosts.

`200`:
```json
{
  "items": [
    {
      "id": "gmail",
      "name": "Gmail",
      "host": "imap.gmail.com",
      "port": 993,
      "ssl": true,
      "appPasswordUrl": "https://myaccount.google.com/apppasswords",
      "help": "Gmail accepts a 16-character App Password (turn on 2-Step Verification first)."
    }
  ]
}
```
- `id` (string): stable slug (`gmail`, `icloud`, `fastmail`, `outlook`,
  `other`). `other` has `host` `""`: the user types it.
- `appPasswordUrl` (string or `null`): where to create an app password.
- `help` (string): one sentence to show under the password field.

Errors: `401 unauthorized`.

### 5.12 POST /api/v1/mailboxes
Bearer token required. Connects a mailbox with an IMAP password (usually an
app password). The same `MailAccountService.connect` as the website's
`POST /mailboxes`: checks the plan cap, logs in over IMAP to verify the
credentials, stores the password encrypted, then imports the most recent
30 INBOX messages before answering. Allow **up to 90 seconds**; the
default client timeout is too short.

Request:
```json
{ "host": "imap.gmail.com", "port": 993, "ssl": true, "username": "ada@example.com", "password": "abcd efgh ijkl mnop" }
```
- `host` (string, required, non-blank, ≤ 255 chars; trimmed).
- `port` (integer, optional, default `993`; 1–65535).
- `ssl` (boolean, optional, default `true`).
- `username` (string, required, non-blank, ≤ 254 chars; trimmed).
- `password` (string, required, non-blank; sent as-is). Never logged or
  returned.

Field rules are the website form's (`MailboxForm`); a violation is
`400 validation_failed` whose `message` names the first problem.

`201`: the Mailbox object (§5.10). If the credentials worked but the
first import failed, the mailbox is still connected and `lastSyncError`
says why — show it, do not treat it as a failure.

Errors: `400 validation_failed`; `403 plan_limit_reached` (already at the
plan's mailbox cap — `message` names the cap; upgrades happen on the
website); `422 mailbox_connect_failed` (IMAP login or connection failed;
`message` includes the server's reason; nothing was stored);
`401 unauthorized`.

### 5.13 POST /api/v1/mailboxes/{id}/sync
Bearer token required. Polls one mailbox now — received and sent mail —
and answers when the poll is done (the website's "Sync now",
`MailboxSyncService`). Allow up to 90 seconds. Request body: none.

`200`:
```json
{ "imported": 2, "mailbox": { …Mailbox object, after the poll… } }
```
- `imported` (integer): messages new to the inbox from this poll.
- A failed poll is **not** an HTTP error: it is `200` with
  `mailbox.lastSyncError` / `errorHint` set, exactly as a scheduled poll
  records it. Clients refetch §5.5 when `imported > 0`.

Errors: `404 not_found` (not one of the user's mailboxes);
`401 unauthorized`.

### 5.14 DELETE /api/v1/mailboxes/{id}
Bearer token required. Disconnects a mailbox (website: "Remove"):
deletes its stored credentials and sync cursor so polling stops.
Conversations already imported stay. If it was the default sending
mailbox, there is no default afterwards. `204`, empty body.

Errors: `404 not_found` (unknown, foreign or already removed);
`401 unauthorized`.

### 5.15 GET /api/v1/account
Bearer token required. The Account screen: the user (same object as
§5.4) plus their paid subscription, summarized exactly as the website's
`/account` page shows it (`AccountBillingSummary`).

`200`:
```json
{
  "user": { …same object as GET /api/v1/me… },
  "billing": { "label": "Pro · Annual", "renewsOn": "2027-06-07", "trialing": false, "canceled": false }
}
```
- `billing` is `null` on the Free plan (no paid subscription row).
- `label` (string): display text, e.g. `Pro · Monthly`,
  `Pro · Annual trial`, `Business · Canceled`. Show it as-is.
- `renewsOn` (calendar date `YYYY-MM-DD` or `null` = not known yet): the
  trial end while `trialing`, else the next renewal. Not meaningful when
  `canceled`.
- `trialing`, `canceled` (boolean).
- Upgrades, cancellation and payment details happen on the website.

Errors: `401 unauthorized`.

### 5.16 GET /api/v1/account/devices
Bearer token required. The user's signed-in apps: every unexpired API
token (one per sign-in, §2), most recently used first. The website's
`/account` "Signed-in apps" section lists the same rows
(`SignedInDeviceService`).

`200`:
```json
{
  "items": [
    {
      "id": 17,
      "deviceName": "ConexusMail for android",
      "signedInAt": "2026-11-19T08:00:00Z",
      "lastUsedAt": "2026-11-19T09:12:00Z",
      "expiresAt": "2026-12-19T08:00:00Z",
      "current": true
    }
  ]
}
```
- `id` (integer): pass to §5.17.
- `deviceName` (string or `null`): what the client sent at sign-in.
- `signedInAt`, `expiresAt` (instants).
- `lastUsedAt` (instant or `null` = not used since sign-in). Updated at
  most every 5 minutes, so treat it as approximate.
- `current` (boolean): the token that made this request. Exactly one row
  is `true`.

Errors: `401 unauthorized`.

### 5.17 DELETE /api/v1/account/devices/{id}
Bearer token required. Signs one device out (revokes its token). Revoking
the `current` device is the same as `POST /auth/logout`: the next request
with this token is `401`. `204`, empty body.

Errors: `404 not_found` (unknown, expired, already revoked, or another
user's); `401 unauthorized`.

### 5.18 POST /api/v1/account/devices/sign-out-others
Bearer token required. Revokes every token of the user except the one
making this request. Request body: none.

`200`:
```json
{ "revoked": 2 }
```
- `revoked` (integer): tokens revoked, `0` when there were none.

Errors: `401 unauthorized`.

### 5.19 GET /api/v1/events
Bearer token required (`Authorization` header, like every `/api/v1`
call). A Server-Sent Events stream of changes to the signed-in user's
conversation list, detailed enough to update list rows in place without
refetching the list.

- `200 text/event-stream`, UTF-8. The server closes the stream after 30
  minutes; clients reconnect (with backoff on errors). A `401` with the
  JSON error envelope (§5.1) is returned instead when the token is bad.
- Comment line `:ping` every 25 s (`mailbox.sse.keepalive-ms`); clients
  ignore comments.
- No event ids and no replay: anything that happened while disconnected
  is lost, so a client refetches the list (§5.5) after every (re)connect.
- Fan-out is in-memory: works on a single backend instance only.

Event `conversations-changed`, sent after a mailbox poll (scheduled, IMAP
IDLE, sync now, or the refresh §5.5 kicks off) commits at least one
imported message:
```
event: conversations-changed
data: {"imported":2,"conversations":[{…§5.5 item…}],"truncated":false,"counts":{"all":12,"unread":3,"awaiting":1,"attachments":0}}
```
- `imported` (integer ≥ 1): messages imported (received + sent).
- `conversations` (array): the current list row of every conversation the
  import touched, exactly as a §5.5 item of the **unscoped** list (so
  `accounts` is filled), newest `lastActivityAt` first, at most 50.
- `truncated` (boolean): `true` when more than 50 conversations changed;
  `conversations` then holds the 50 most recent and the client should
  refetch the list instead.
- `counts` (object): §5.5 `counts` for the **unscoped** list after the
  import.

Client rule: in the unscoped list with filter `all` and no `q`, replace
the row with the same `key` (or insert it) and keep the list ordered by
`lastActivityAt` desc; update `counts`. In any other view (a search, a
filter, a mailbox scope) the rows may not belong in it, so refetch.
Events are not sent for the user's own sends (the send response already
carries the message, §5.8) or for mark-read (§5.7).

Errors: `401 unauthorized`.

## 6. Planned (not implemented)

Intended resources, in build order (see the mobile repo's
`docs/DESIGN.md` §ROADMAP):
1. Push notifications when the app is closed (the event stream only
   reaches an open app).
2. Attachments on send (multipart), and an idempotency key for sends.
3. Account changes from the app: display name, password, email (website
   only today).
