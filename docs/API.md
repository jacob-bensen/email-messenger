# API.md — canonical mobile API contract

Contract shared by `email-messenger` (backend) and `conexus-mail-mobile`
(Flutter mobile client). Both sides implement to this file exactly.

## Status

**No REST API is implemented yet.** As of 2026-08-29 the backend's HTTP
surface is entirely Thymeleaf, SSE and one ad-hoc JSON endpoint
(`POST /chats/refresh`). Everything below is the **planned** contract for
the mobile client. Endpoints will land under the `/api/v1/**` prefix; the
web app keeps its existing paths untouched.

Every mobile endpoint MUST reuse the same service-layer method the
Thymeleaf equivalent uses. Duplicate business logic between the API and
web paths is a bug.

## Conventions

- Base path: `/api/v1`.
- JSON only, UTF-8, `Content-Type: application/json`.
- Timestamps: ISO-8601 with offset (e.g. `2026-08-29T12:00:00Z`). Server
  is authoritative — the client never sets `sent_at` or `created_at`.
- IDs: server-generated `int64` (matches JPA `BIGINT`) except
  `conversation_key` which is a stable opaque string derived from the
  participant set.
- Pagination: cursor-based. Responses that paginate return
  `{ "items": [...], "next_cursor": "..." | null }`. Clients pass
  `?cursor=…&limit=…` (`limit` capped at 100, default 30).
- Errors: uniform envelope
  `{ "error": { "code": "SNAKE_CASE_CODE", "message": "human string",
  "details"?: {...} } }`. Standard HTTP status codes; auth failures are
  `401` with `code: "UNAUTHENTICATED"`, plan-limit blocks are `402` with
  `code: "PLAN_LIMIT"`, permission denials `403` with `code: "FORBIDDEN"`.

## Authentication

- **Bearer tokens.** Clients send `Authorization: Bearer <access_token>`.
- **Not sessions.** The mobile client does not use the web session
  cookie; the same `users` table backs both.
- **Access + refresh.** Access tokens are short-lived (default 30 min);
  refresh tokens rotate on use.
- **OIDC.** Native Google sign-in delivers a Google ID token that the
  server exchanges for its own access + refresh pair
  (`POST /api/v1/auth/google`).

### `POST /api/v1/auth/login`
Body: `{ "email": string, "password": string }`
200: `{ "access_token": string, "refresh_token": string,
"expires_in": int, "user": User }`
401: `INVALID_CREDENTIALS`; 403 `EMAIL_NOT_VERIFIED`; 429
`LOGIN_THROTTLED`.

### `POST /api/v1/auth/google`
Body: `{ "id_token": string }` (Google ID token from the native flow)
200: same shape as `/auth/login`.

### `POST /api/v1/auth/refresh`
Body: `{ "refresh_token": string }`
200: `{ "access_token": string, "refresh_token": string,
"expires_in": int }`
401: `INVALID_REFRESH_TOKEN`.

### `POST /api/v1/auth/logout`
Body: `{ "refresh_token": string }` — server revokes it.
204.

### `POST /api/v1/auth/register`
Body: `{ "email": string, "password": string, "display_name"?: string }`
201: `{ "user": User, "verification_required": true }`
409 `EMAIL_TAKEN`; 422 `VALIDATION`.

## User

### `GET /api/v1/me`
200: `User` (see schema below).

### `PATCH /api/v1/me`
Body (any subset): `{ "display_name"?, "email_verified"?: never,
"mail_preferences"?: MailPreferences }`.
200: `User`.

## Mailboxes

Owned per user. `conexus-mail-mobile` never handles OAuth secrets — the
Gmail connect flow opens a browser to the backend, which redirects back
to a custom URL scheme (`conexusmail://oauth/return`).

### `GET /api/v1/mailboxes`
200: `{ "items": [Mailbox] }`.

### `POST /api/v1/mailboxes/imap`
Body: `{ "email": string, "host": string, "port": int, "username": string,
"password": string, "use_ssl": bool }`
201: `Mailbox`.

### `POST /api/v1/mailboxes/gmail/oauth/start`
200: `{ "auth_url": string }` — client opens in an external browser; the
backend completes the exchange server-side and pushes a
`mailbox.connected` event over SSE.

### `POST /api/v1/mailboxes/{id}/sync`
Kicks an immediate poll. 202.

### `PATCH /api/v1/mailboxes/{id}/signature`
Body: `{ "signature": string }`. 200: `Mailbox`.

### `DELETE /api/v1/mailboxes/{id}` — 204.

## Chats (conversations)

### `GET /api/v1/chats`
Query: `?cursor=…&limit=…&unread=true|false&saved_search_id=…`
200: `{ "items": [ChatSummary], "next_cursor": string | null }`
Ordered by `last_message_at DESC`. `ChatSummary` fields:
`conversation_key`, `participants`, `subject_hint`, `last_snippet`,
`last_message_at`, `unread_count`, `thread_badge_count`, `avatar_uri`.

### `GET /api/v1/chats/{conversation_key}`
Full open-pane payload:
`{ "conversation_key": string, "participants": [Participant],
"signature_card": ContactCard | null, "messages": [Message],
"threads": [ThreadRef], "next_cursor": string | null }`
`messages` paginate: `?before_id=…&limit=…` returns older bubbles.

### `POST /api/v1/chats/{conversation_key}/send`
Body: `{ "body_plain": string, "body_html"?: string,
"reply_to_message_id"?: int64, "attachments"?: [AttachmentUploadRef],
"mail_account_id": int64 }`
202: `MessageOutbound` (status `sending`; delivery states flow over SSE).
402 `PLAN_LIMIT` if the send would exceed the account's plan cap.

### `POST /api/v1/chats/{conversation_key}/mark-read`
Body: `{ "up_to_message_id": int64 }`. 204.

### `POST /api/v1/chats/refresh`
Body: `{ "mail_account_ids"?: [int64] }`. 202. Enqueues a fetch. (Mirrors
the ad-hoc web endpoint of the same name.)

## Attachments

### `POST /api/v1/attachments`
Multipart upload; returns an `AttachmentUploadRef` the client hands to
`send`.
### `GET /api/v1/attachments/{id}/blob`
Returns raw bytes. 404 if `blob_ref` is null on the record (matches the
web limitation until the blob-store roadmap item lands).

## Saved searches

### `GET /api/v1/saved-searches` — 200: `{ "items": [SavedSearch] }`.
### `POST /api/v1/saved-searches` — Body `{ "name", "query" }`, 201.
### `DELETE /api/v1/saved-searches/{id}` — 204.

## Digests

### `GET /api/v1/digest/preferences` — 200: `DigestPreferences`.
### `PATCH /api/v1/digest/preferences` — Body: subset. 200: same.

## Billing

Read-only from mobile. Purchases run through the web billing portal or a
native store integration (**TBD**).

### `GET /api/v1/billing/plan` — 200: `{ "plan": "free"|"pro"|"business",
"trial_ends_at": string|null, "current_period_end": string|null,
"cancel_at": string|null }`.

### `POST /api/v1/billing/portal-url` — 200: `{ "url": string }` opens the
Stripe customer portal in an external browser.

## Live events (SSE)

### `GET /api/v1/events`
Bearer-authenticated Server-Sent Events stream. Reuses the same event
dispatcher as `/chats/events`. Event `event:` values:
- `new_mail` — data: `{ "conversation_key", "message_id",
  "chat_summary": ChatSummary }`
- `mail_sent` — data: `MessageOutbound` update.
- `mailbox_connected` — data: `Mailbox`.
- `mailbox_error` — data: `{ "mail_account_id", "error_code",
  "message" }`.

Clients should reconnect with `Last-Event-ID` on drop.

## Push notifications (Web Push / APNs / FCM)

**TBD.** Blocked on the roadmap's "Live delivery, part 2" item. The
mobile registration endpoint is expected to be
`POST /api/v1/push/subscribe` accepting the platform-specific token, but
schema is not final.

## Schemas

```jsonc
User {
  "id": int64,
  "email": string,
  "email_verified": bool,
  "display_name": string | null,
  "plan": "free"|"pro"|"business",
  "created_at": timestamp,
  "mail_preferences": MailPreferences
}

MailPreferences {
  "digest_enabled": bool,
  "digest_day": "mon"|"tue"|"wed"|"thu"|"fri"|"sat"|"sun",
  "notification_opt_in": bool
}

Mailbox {
  "id": int64,
  "email": string,
  "auth_type": "imap"|"gmail_oauth",
  "polling_policy": "poll"|"idle",
  "connected": bool,
  "last_error": string | null,
  "signature": string | null
}

Participant {
  "id": int64,
  "email": string,
  "display_name": string | null,
  "avatar_uri": string | null
}

ChatSummary {
  "conversation_key": string,
  "participants": [Participant],
  "subject_hint": string | null,
  "last_snippet": string,
  "last_message_at": timestamp,
  "unread_count": int,
  "thread_badge_count": int,
  "avatar_uri": string | null
}

Message {
  "id": int64,
  "thread_id": int64,
  "thread_badge": int,
  "sender": Participant,
  "recipients": { "to": [Participant], "cc": [Participant] },
  "body_plain": string,
  "body_html": string | null,
  "sent_at": timestamp,
  "attachments": [AttachmentRef],
  "is_outbound": bool,
  "delivery_state":
    "sending" | "sent" | "delivered" | "failed" | null
}

MessageOutbound {
  "id": int64,
  "conversation_key": string,
  "message_id_header": string | null,
  "delivery_state": "sending"|"sent"|"delivered"|"failed",
  "failure_reason": string | null
}

AttachmentRef {
  "id": int64,
  "filename": string,
  "mime_type": string | null,
  "size_bytes": int64 | null,
  "downloadable": bool
}

AttachmentUploadRef { "id": int64 }

SavedSearch {
  "id": int64,
  "name": string,
  "query": string,
  "unread_count": int,
  "last_viewed_at": timestamp | null
}

DigestPreferences {
  "enabled": bool,
  "day": string,
  "hour_local": int
}

ContactCard {
  "participant": Participant,
  "phone": string | null,
  "title": string | null,
  "company": string | null,
  "sites": [string]
}

ThreadRef {
  "id": int64,
  "badge_number": int,
  "subject": string,
  "message_count": int
}
```

## Versioning

Contract-breaking changes bump the URL prefix (`/api/v2/**`). Additive
changes stay under `/api/v1`. Removing a field is contract-breaking.
Adding a nullable field is not.

## Changelog

- **2026-08-29** — initial draft. No endpoints implemented yet; contract
  captured so both repos can build against the same shapes.
