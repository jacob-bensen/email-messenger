# ConexusMail REST API (v1)

**Status:** DRAFT. This is the canonical contract between the backend
(this repo) and the Flutter mobile client
(sibling repo `conexus-mail-mobile`). The server today only exposes
Thymeleaf pages plus two `@ResponseBody` endpoints — `POST /chats/refresh`
and `POST /webhooks/stripe`. Every endpoint listed below marked
**PLANNED** does not exist in code yet; it is the shape we intend to
implement one feature at a time, updating this file BEFORE the code.

Both repos MUST match this file exactly. When they don't, the mismatch
goes in `INTERNAL_TODO.md` in both repos, ordered by severity.

## Conventions

- Base path: `/api/v1`.
- Media type: `application/json; charset=utf-8` for both request and
  response.
- Auth: bearer token (`Authorization: Bearer <token>`), obtained from
  `POST /api/v1/auth/login`. Tokens are opaque strings; the server may
  rotate them without notice. **Mechanism (JWT vs. DB-backed) is a TBD
  server implementation detail** — clients treat them as opaque.
- IDs are `long` server-side; represented as JSON numbers.
- Timestamps: ISO-8601 UTC with `Z` suffix, second precision.
- Errors follow RFC 7807 (`application/problem+json`) with fields:
  `type`, `title`, `status`, `detail`, and optional `code` for
  machine-readable enum values.
- Pagination: page-based, `?page=0&size=30`, response includes
  `page`, `size`, `totalElements`, `totalPages`.

## Error codes

| HTTP | `code`                    | Meaning                                    |
| ---- | ------------------------- | ------------------------------------------ |
| 400  | `validation_failed`       | Request body failed validation             |
| 401  | `unauthenticated`         | Missing / invalid bearer token             |
| 402  | `plan_limit_exceeded`     | Free / Pro cap hit (see `PlanLimitKind`)   |
| 403  | `forbidden`               | Authenticated but not allowed              |
| 404  | `not_found`               | Resource missing or not owned by caller    |
| 409  | `conflict`                | Version conflict (see `ETag` where used)   |
| 429  | `throttled`               | Rate / login-throttle hit                  |
| 502  | `imap_unreachable`        | Provider IMAP is down or credentials stale |

---

## Auth

### `POST /api/v1/auth/login` — **PLANNED**

Request:
```json
{ "email": "user@example.com", "password": "…" }
```
Response `200`:
```json
{
  "token": "…",
  "expiresAt": "2026-09-05T14:00:00Z",
  "user": { "id": 42, "email": "user@example.com", "displayName": "Ada" }
}
```
Failure: `401 { "code": "unauthenticated" }` when credentials wrong;
`429 { "code": "throttled" }` after N failures (matches
`LoginThrottleService`).

### `POST /api/v1/auth/logout` — **PLANNED**

Invalidates the bearer token. `204` on success.

### `POST /api/v1/auth/oauth/google` — **PLANNED**

Exchanges a Google ID token from the mobile Google Sign-In SDK for a
ConexusMail bearer token. Mirrors `GoogleOidcUserService` +
`OAuth2ProvisioningService`.

Request:
```json
{ "idToken": "…" }
```
Response: same as `/auth/login`.

### `GET /api/v1/me` — **PLANNED**

Returns the current user + subscription summary + feature flags. Backed
by `UserService` + `AccountService`.

---

## Mailboxes

### `GET /api/v1/mailboxes` — **PLANNED**

Lists the caller's mail accounts (mirrors `MailAccountService`). Each
entry:
```json
{
  "id": 7,
  "emailAddress": "ada@gmail.com",
  "authType": "GMAIL_OAUTH",
  "syncFrequency": "EVERY_5_MIN",
  "lastPolledAt": "2026-08-29T15:00:00Z",
  "signatureHtml": "…",
  "reconnectRequired": false
}
```

### `POST /api/v1/mailboxes` — **PLANNED**

Adds an IMAP-password mailbox. Gmail OAuth mailboxes go through the
in-app browser to `/mailboxes/gmail/connect` (web flow reused; mobile
opens it in a Custom Tab / ASWebAuthenticationSession).

### `POST /api/v1/mailboxes/{id}/sync` — **PLANNED**

Force-polls the given mailbox. Same semantics as
`POST /mailboxes/{id}/sync` on the web.

### `DELETE /api/v1/mailboxes/{id}` — **PLANNED**

Removes the mailbox and its threads (mirrors `POST /mailboxes/{id}/delete`).

---

## Conversations (chats)

### `GET /api/v1/chats?page=0&size=30&filter=…` — **PLANNED**

Returns the user's conversation list, most-recent first, keyed by
participant set (matches `ConversationListService`).

Item shape:
```json
{
  "conversationKey": "b3a1…",
  "participants": [
    { "email": "ada@…", "displayName": "Ada", "avatarInitials": "A" }
  ],
  "lastMessageAt": "2026-08-29T14:55:00Z",
  "lastMessagePreview": "See you Friday.",
  "unreadCount": 2,
  "messageCount": 17
}
```

### `GET /api/v1/chats/{conversationKey}` — **PLANNED**

Returns the merged, IM-transformed timeline for a conversationKey
(mirrors `ChatService` + `IMTransformService`).

Response includes bubble runs already grouped by consecutive sender,
each bubble carrying `id`, `sentAt`, `senderEmail`, `outbound`,
`bodyPlain`, `bodyHtmlSanitized`, `attachments[]`, `threadRef`
(subject + Message-ID for the "reply to specific bubble" feature).

### `POST /api/v1/chats/refresh` — **PLANNED**

Force-poll of all connected mailboxes. Returns per-mailbox unread
counts. Direct mirror of the current
`POST /chats/refresh` (`@ResponseBody`) endpoint — the only existing
JSON endpoint of the chat surface.

### `POST /api/v1/chats/{conversationKey}/messages` — **PLANNED**

Sends a reply into the conversation. Uses `ReplyService` +
`OutboundMessageService`.

Request:
```json
{
  "mailboxId": 7,
  "bodyPlain": "…",
  "bodyHtml": "…",
  "inReplyToMessageId": "<abc@…>",
  "attachments": []   // stub; attachment upload is a separate endpoint TBD
}
```
Response `201`: the freshly-inserted bubble in the same shape as
`GET /api/v1/chats/{key}` items.

### `POST /api/v1/chats/new` — **PLANNED**

Starts a new conversation (analog of `POST /chats/new`).

---

## Streaming

### `GET /api/v1/chats/events` — **PLANNED**

Server-Sent Events (`text/event-stream`) or WebSocket (choice TBD).
Publishes `NewMailEvent` for the caller. The mobile client can also
subscribe to Web Push (roadmap item — Live delivery, part 2) when
backgrounded.

---

## Billing (read-only from mobile)

### `GET /api/v1/billing/summary` — **PLANNED**

Returns plan, renewal date, trial state, and the upgrade / manage URLs
(the mobile app opens those in a browser rather than embedding Stripe).

---

## What already exists as JSON today

- `POST /chats/refresh` (`ConversationController#refresh`,
  `@ResponseBody`) — returns `Map<String,Integer>` of unread counts by
  bucket. **Not** namespaced under `/api/v1`. To be replaced by
  `POST /api/v1/chats/refresh` above; keep the current endpoint working
  until the web UI is migrated.
- `POST /webhooks/stripe` (`BillingWebhookController`) — Stripe → server
  only. Not part of the mobile contract.

Everything else is Thymeleaf HTML.
