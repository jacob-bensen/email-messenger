# API contract (single source of truth)

This file is the **shared contract** between the backend and the ConexusMail
mobile client. Both sides implement against exactly what's here.

## Status: bootstrap

The backend today is a Thymeleaf server-rendered web app. It has **no JSON
REST API** for third-party clients. Adding one is the largest gap between
the backend and the mobile client — every mobile feature needs one or more
endpoints defined here first.

Until each endpoint below moves out of the **PLANNED** list into the
**LIVE** list, the mobile client must not call it.

Base URL for JSON endpoints: `/api/v1/` (planned).

---

## LIVE endpoints (available today)

### `POST /chats/refresh`
Session-authenticated. Foreground heartbeat — triggers a synchronous
mailbox refresh for the current user's due accounts and returns how many
new messages were imported.

- Auth: session cookie (form login).
- Request body: none.
- Response `200 OK`: `{"imported": <int>}`

Not usable from the mobile app until a token-auth mechanism exists (see
below). This endpoint was designed for the open web page.

### `GET /chats/events`
Session-authenticated. Server-Sent Events stream that emits a `new-mail`
event whenever an import (polling or IDLE) attributes new messages to the
current user. Payload: `{"conversationKey": "<key>", "count": <int>}`.

Same session-cookie limitation as `/chats/refresh`.

### `POST /billing/webhook`
Stripe → backend webhook. Not for client use. Signed with Stripe's
webhook secret; CSRF-exempt.

---

## PLANNED endpoints (must be built before the mobile client can use them)

All PLANNED endpoints are TBD until they land. When one is implemented,
promote it to LIVE with the actual request/response schema. Never let
what's LIVE diverge from what the server actually does.

### Auth (token-based, mobile-ready)

- `POST /api/v1/auth/login` — email + password → access token + refresh
  token. TBD schema; must not tie the mobile client to the browser
  `JSESSIONID` cookie.
- `POST /api/v1/auth/refresh` — refresh token → new access token.
- `POST /api/v1/auth/logout` — invalidate refresh token.
- `POST /api/v1/auth/register` — mirror of the web `POST /register`, JSON
  in / JSON out.
- Google OAuth for mobile — decide flow (in-app WebView redirect back to
  a deep link, or native Google Sign-In → ID token → backend exchange).

### Account

- `GET /api/v1/me` — current user profile: id, email, display name, plan,
  emailVerified, defaultMailAccountId.
- `PATCH /api/v1/me` — update display name / appearance settings.
- `POST /api/v1/me/password` — change password.

### Mailboxes

- `GET /api/v1/mailboxes` — list connected mail accounts (id, username,
  provider, authType, lastSeenAt, signature).
- `POST /api/v1/mailboxes/imap` — connect a new IMAP mailbox.
- `POST /api/v1/mailboxes/{id}/sync` — synchronous refresh (mirror of
  the existing web `POST /mailboxes/{id}/sync`).
- `POST /api/v1/mailboxes/{id}/signature` — update signature.
- `DELETE /api/v1/mailboxes/{id}` — disconnect.
- Gmail OAuth start / callback — needs a mobile-friendly redirect.

### Chats

- `GET /api/v1/chats?mailbox=&filter=&q=&cursor=` — paginated chat list;
  response mirrors `ConversationListService.list(...)` output.
- `GET /api/v1/chats/{conversationKey}` — one chat: members,
  merged-timeline messages, signature rail data.
- `POST /api/v1/chats/{conversationKey}/messages` — send message
  (matches `POST /chats/{key}/send`). Multipart for attachments.
- `POST /api/v1/chats` — start a new chat (recipients + subject + body +
  attachments; matches `POST /chats/new`).
- `POST /api/v1/chats/{conversationKey}/read` — mark read.
- `GET /api/v1/chats/events` — SSE or WebSocket stream mirroring
  `/chats/events`, with token auth.
- Attachment download endpoint — blocked on the backend's `blob_ref`
  storage work (currently always null).

### Billing

- `GET /api/v1/billing/summary` — current plan, subscription status,
  period end, cancellation state.
- `POST /api/v1/billing/checkout` — start Stripe Checkout; response is
  the Checkout URL to open in a native browser.
- `POST /api/v1/billing/portal` — Stripe Customer Portal URL.

### Saved searches, digests, teams

Mirror the existing web controllers; JSON schemas TBD when implemented.

---

## Cross-cutting rules

- **Server-authoritative.** The mobile client never computes plan limits,
  conversation keys, unread counts, or delivery state on its own. It
  displays what the server returns.
- **Errors.** Use HTTP status codes with a JSON body of
  `{"error": {"code": "<slug>", "message": "<human>"}}`. Codes stable
  across versions.
- **Dates.** ISO-8601 UTC strings (`2026-08-29T12:34:56Z`).
- **IDs.** All entity ids are numeric (JPA Long) — the mobile client
  receives them as JSON numbers and stores them as `int` (Dart `int` is
  64-bit).
- **Conversation keys** are opaque strings (participant-set hashes). Never
  parse them client-side.
- **Pagination.** Cursor-based (opaque `cursor` string), not offset-based.
- **CORS.** Not needed for a native mobile client, but if a web SPA
  eventually consumes these endpoints CORS will need to be configured.

## When adding a new endpoint

1. Write the entry here first (method, path, auth, request/response,
   errors) BEFORE writing the controller or the client call.
2. Implement the server controller as a thin JSON adapter over the
   existing service layer — never duplicate logic that the Thymeleaf
   controllers already delegate to.
3. Add or update the Dart DTO + repository call in
   `conexus-mail-mobile/lib/data/` to match this file byte-for-byte.
4. Promote the entry from PLANNED to LIVE in the same commit.
