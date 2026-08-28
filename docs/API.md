# API contract — email-messenger backend

**Status:** DRAFT / BOOTSTRAP. As of this doc's creation the backend is
entirely a Thymeleaf web app; there is no REST/JSON API for the mobile
client yet. This document has two sections:

1. **What exists today** — the handful of JSON-shaped endpoints that
   currently ship, so we don't accidentally break them.
2. **Planned mobile API** — the endpoints the [conexus-mail-mobile]
   Flutter client will call, to be added incrementally. Every mobile
   feature run must update this section BEFORE the server or client
   code lands, and both implementations must match it exactly.

The full HTML-page inventory (Thymeleaf routes, forms) lives in
`docs/WEB.md`, not here.

[conexus-mail-mobile]: https://github.com/jacob-bensen/conexus-mail-mobile

## Conventions

- Base URL: `https://<host>/api/v1` (planned; no `/api` routes exist
  yet). All planned endpoints below assume this prefix unless otherwise
  noted.
- Content type: `application/json; charset=utf-8` for both request and
  response bodies unless the endpoint is `multipart/form-data`
  (attachments) or a stream.
- Timestamps: ISO 8601 with a timezone offset, e.g.
  `"2026-08-28T14:03:11Z"`.
- IDs: server-generated `long` (JSON number) unless noted.
  ConversationKey is a `string` (opaque hash).
- Errors: JSON object `{ "error": "<code>", "message": "<human>",
  "details": { ... } }`, HTTP status from the table below.
- Auth: `Authorization: Bearer <token>` on every request except
  `/auth/*` and `/health`. Token issuance is TBD (see gap list); the
  Thymeleaf app uses cookie sessions, which the mobile client will not
  use.
- Pagination: cursor-based. Response envelopes for list endpoints:
  `{ "items": [...], "nextCursor": "<opaque>" | null }`. `?cursor=` on
  the request.

### Standard error codes

| HTTP | `error`                    | Meaning                                        |
| ---- | -------------------------- | ---------------------------------------------- |
| 400  | `bad_request`              | Malformed body or params.                      |
| 401  | `unauthenticated`          | Missing / invalid token.                       |
| 403  | `forbidden`                | Authenticated but not authorized.              |
| 403  | `plan_limit_exceeded`      | Blocked by the caller's plan.                  |
| 404  | `not_found`                | Resource missing.                              |
| 409  | `conflict`                 | Constraint violation (dup email, etc.).        |
| 413  | `payload_too_large`        | Attachment upload over limit.                  |
| 422  | `validation_failed`        | JSR-303 field errors in `details.fields`.      |
| 429  | `rate_limited`             | Retry-After header set.                        |
| 502  | `mail_provider_error`      | IMAP / SMTP upstream failure.                  |
| 502  | `billing_provider_error`   | Stripe upstream failure.                       |
| 500  | `internal`                 | Everything else.                               |

---

## 1. Endpoints that exist today

These are the JSON-shaped surfaces already in the codebase. Do not
break them until the mobile API replaces them.

### `POST /billing/webhook`

Stripe webhook receiver. **Not for clients.** Body is Stripe's raw
event JSON, verified via the `Stripe-Signature` header. Returns 200
`ok` on success, 400 on bad signature, 500 on retryable error, 503 if
Stripe is not configured. Public (permitAll).

### `POST /chats/refresh`

Cookie-session authenticated. Triggers a foreground poll of the caller's
mailboxes.

Response:

```json
{ "imported": 3 }
```

### `GET /chats/events`

Server-Sent Events stream, `Content-Type: text/event-stream`.
Cookie-session authenticated. Emits `new-mail` events when a
`NewMailEvent` fires for the caller's user id.

Event payload (SSE `data:` line, JSON):

```json
{ "conversationKey": "abc123", "unreadCount": 4 }
```

---

## 2. Planned mobile API (all TBD, none implemented)

Nothing below exists in code yet. Each subsection is a placeholder the
next feature run should replace with the real shapes, then implement in
both the backend controllers and the Flutter networking layer in the
same run. Do NOT let this drift.

### 2.1 Auth

- `POST /auth/register` — email + password + displayName. Returns
  token + user summary.
- `POST /auth/login` — email + password. Returns token + user summary.
- `POST /auth/google` — Google ID token from the mobile Google Sign-In
  SDK, verified server-side. Returns token + user summary.
- `POST /auth/logout` — revokes the current token.
- `POST /auth/password/forgot`, `POST /auth/password/reset` — same
  flows the Thymeleaf app implements.
- `POST /auth/email/verify/resend`.

Token strategy: TBD. Leading candidates: short-lived JWT + refresh
token, or opaque session token backed by a `mobile_sessions` table.
Whichever ships must be implemented server-side, not client-side.

### 2.2 Account

- `GET /me` — user profile, plan summary, feature flags.
- `PATCH /me/password`, `PATCH /me/email`.
- `GET /me/subscription`, `GET /me/plan-limits`.

### 2.3 Mailboxes

- `GET /mailboxes` — connected mailboxes.
- `POST /mailboxes` — connect an IMAP+password mailbox
  (host, port, ssl, username, password, provider).
- `POST /mailboxes/gmail/authorize` — returns a Google authorization
  URL for the mobile OAuth flow (mobile handles the redirect via a
  custom URL scheme, then posts the code back).
- `POST /mailboxes/gmail/exchange` — trades the OAuth code for a
  stored refresh token.
- `POST /mailboxes/{id}/sync` — foreground sync one mailbox.
- `POST /mailboxes/{id}/signature`, `DELETE /mailboxes/{id}`.
- `PATCH /mailboxes/preferences` — sync frequency, default sending
  mailbox.

### 2.4 Chats (conversations by participant set)

- `GET /chats?q=&mailbox=&filter=&cursor=` — list conversations for
  the `/chats` rail. Each item: conversationKey, displayName,
  latestSnippet, lastMessageAt, unreadCount, participantAvatars.
- `GET /chats/{key}?mailbox=&cursor=` — full conversation, oldest
  first, page from `cursor`. Items are bubbles (see 2.5).
- `POST /chats/{key}/read` — mark read up through the newest visible
  message.
- `POST /chats/new` — start a new conversation. Body: to[], subject?,
  body, attachments? (multipart or two-step upload TBD).
- `POST /chats/{key}/send` — reply into an existing conversation. Body:
  body, asReply (bool), subject? (only when starting a new thread
  inside the same conversationKey), attachments?.

### 2.5 Messages / bubbles

- `GET /messages/{id}` — full message detail (headers, HTML body,
  attachments list).
- `GET /messages/{id}/attachments/{attId}` — download attachment
  bytes. **Blocked:** attachment blob storage is not implemented yet
  (`blob_ref` is always null); see backend roadmap.

### 2.6 Search & saved searches

- `GET /search?q=&from=&since=&unread=&attachments=&cursor=`.
- `GET /searches`, `POST /searches`, `DELETE /searches/{id}`.

### 2.7 Live delivery

- `GET /events` — SSE (or WebSocket) stream of `new-mail`,
  `conversation-read`, `subscription-changed`. Auth via
  `Authorization: Bearer` in the initial handshake or `?token=` query
  param (SSE can't set headers from EventSource, so query-string is
  likely).
- `POST /push-subscriptions` — register an APNs / FCM token so the
  server can push notifications when no client is connected.

### 2.8 Billing

- `GET /plans` — public plan catalog (name, monthly/annual price,
  features).
- `POST /billing/checkout-session` — returns a Stripe Checkout URL
  the mobile client opens in an in-app browser.
- `POST /billing/portal-session` — returns a Stripe Portal URL.

### 2.9 Health

- `GET /health` — `{ "status": "ok" }`. Public. Backed by
  `actuator/health`.

---

## Contract change process

For every feature run that touches the client / server boundary:

1. Update this file first with the exact request / response shapes.
2. Implement the backend endpoint + tests.
3. Implement the Flutter DTO + call site + tests.
4. Verify DTO fields match on both sides byte-for-byte.
5. Commit both repos in the same run.

Never merge one side without the other.
