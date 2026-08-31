# API contract — ConexusMail (canonical)

**Status as of 2026-08-31: no REST API is implemented yet.** The backend
currently serves only server-rendered Thymeleaf pages (see `docs/WEB.md`).
This document defines the target contract for the `conexus-mail-mobile`
Flutter client; it is the single source of truth once endpoints ship — the
server and mobile client must match this file exactly.

Everything below is a proposal until an endpoint's row is marked
**Implemented**.

## Conventions

- Base path: `/api/v1`.
- Content type: `application/json; charset=utf-8` for both requests and
  responses, unless explicitly noted (SSE uses `text/event-stream`).
- Auth: bearer token in `Authorization: Bearer <token>`. Tokens are issued
  by `POST /api/v1/auth/login` and are opaque (no JWT claims the client
  should read). Session cookies used by the website are NOT valid for the
  API; the mobile client uses tokens only.
- Timestamps: ISO 8601 with offset, e.g. `2026-08-31T14:22:05Z`.
- IDs: server-assigned integers unless otherwise noted; the client treats
  them as opaque `int64`.
- Pagination: cursor-based. Endpoints that page return
  `{ "items": [...], "nextCursor": "..." | null }`. Pass the cursor back as
  `?cursor=...`. Page size is server-decided; the client MAY hint with
  `?limit=` but the server clamps.
- Errors: JSON body `{ "code": "SNAKE_CASE", "message": "human-readable",
  "fields": { "field": "reason" } | null }`. HTTP status matches the class of
  error (400 validation, 401 auth, 403 forbidden, 404 missing, 409 conflict,
  422 semantic, 429 rate-limited, 5xx server). Error codes below are
  proposed; add new ones by editing this file first.
- Idempotency: for mutations that could be retried after network loss
  (send-reply, delete-mailbox), the client sends
  `Idempotency-Key: <uuid>`; the server dedupes within 24h.

## Auth

| Method | Path                        | Purpose                          | Status |
| ------ | --------------------------- | -------------------------------- | ------ |
| POST   | `/auth/register`            | Create an account (email+pass)   | TBD    |
| POST   | `/auth/login`               | Exchange creds for a token       | TBD    |
| POST   | `/auth/logout`              | Revoke the current token         | TBD    |
| POST   | `/auth/google`              | Exchange Google ID token → token | TBD    |
| POST   | `/auth/password/forgot`     | Email a reset link               | TBD    |
| POST   | `/auth/password/reset`      | Consume a reset token            | TBD    |
| POST   | `/auth/verify-email`        | Consume an email-verify token    | TBD    |
| GET    | `/me`                       | Current user profile + plan      | TBD    |

`POST /auth/login`
```
Request:  { "email": "…", "password": "…" }
Response: { "token": "…", "user": <User> }
Errors:   INVALID_CREDENTIALS (401), EMAIL_NOT_VERIFIED (403),
          RATE_LIMITED (429)
```

`User` shape:
```
{
  "id": 42,
  "email": "ada@example.com",
  "displayName": "Ada Lovelace" | null,
  "emailVerified": true,
  "plan": "FREE" | "PRO" | "BUSINESS",
  "createdAt": "2026-01-02T…"
}
```

## Mailboxes

| Method | Path                              | Purpose                     | Status |
| ------ | --------------------------------- | --------------------------- | ------ |
| GET    | `/mailboxes`                      | List connected mailboxes    | TBD    |
| POST   | `/mailboxes`                      | Connect an IMAP+SMTP mailbox| TBD    |
| POST   | `/mailboxes/gmail/start`          | Begin Gmail OAuth flow      | TBD    |
| POST   | `/mailboxes/{id}/sync`            | Trigger a manual sync       | TBD    |
| DELETE | `/mailboxes/{id}`                 | Disconnect a mailbox        | TBD    |
| PUT    | `/mailboxes/{id}/signature`       | Update the outgoing signature| TBD   |

`Mailbox` shape:
```
{
  "id": 7,
  "address": "ada@example.com",
  "provider": "IMAP" | "GMAIL",
  "displayName": "Ada @ Work",
  "authType": "PASSWORD" | "OAUTH",
  "state": "ACTIVE" | "NEEDS_RECONNECT" | "ERROR",
  "lastSyncedAt": "2026-08-30T…" | null,
  "signature": "…" | null
}
```

## Chats (the merged texting-style view)

The `conversationKey` is a server-computed hash of the sorted, lowercased
non-owner participants across the underlying threads. All threads that
share it appear as one chat.

| Method | Path                                     | Purpose                              | Status |
| ------ | ---------------------------------------- | ------------------------------------ | ------ |
| GET    | `/chats`                                 | List chats (rail)                    | TBD    |
| GET    | `/chats/{key}`                           | Fetch one chat's bubble runs         | TBD    |
| POST   | `/chats/{key}/send`                      | Send a reply into the chat           | TBD    |
| POST   | `/chats/{key}/read`                      | Mark chat as read                    | TBD    |
| GET    | `/chats/events`                          | SSE stream of new-mail events        | TBD    |

`ChatListItem`:
```
{
  "key": "b3f5…",                    # conversationKey
  "participants": [<Participant>],
  "subjectHint": "Trip to Kyoto",
  "lastMessagePreview": "sounds great, see you then",
  "lastMessageAt": "2026-08-30T…",
  "unreadCount": 3
}
```

`ChatDetail`:
```
{
  "key": "b3f5…",
  "participants": [<Participant>],
  "bubbleRuns": [ <BubbleRun> ],
  "threads": [ { "id": 1, "subject": "…", "messageCount": 4 } ]
}
```

`BubbleRun` (matches server-side `SenderBubbleRun`):
```
{
  "senderId": 12,
  "senderDisplayName": "Ada",
  "outbound": false,
  "bubbles": [
    {
      "messageId": 99,
      "bodyMarkdown": "…",
      "bodyHtml": "…",
      "sentAt": "2026-08-30T…",
      "attachments": [<AttachmentRef>],
      "threadId": 1,
      "messageIdHeader": "<abc@example.com>"
    }
  ]
}
```

`POST /chats/{key}/send`
```
Request:  {
  "body": "Sounds good.",
  "replyToMessageIdHeader": "<abc@example.com>" | null,
  "attachments": [ { "uploadId": "…" } ]
}
Response: 202 Accepted, { "message": <BubbleMessage>, "deliveryState":
          "SENDING" | "SENT" }
Errors:   NO_CONNECTED_MAILBOX (409), PLAN_LIMIT_EXCEEDED (403),
          MAILBOX_ERROR (502)
```

`GET /chats/events` (SSE) emits:
```
event: new-mail
data: { "key": "b3f5…", "threadId": 1, "messageId": 99 }
```
The client MUST NOT trust these payloads for state; on receipt it refetches
`GET /chats/{key}` (or the rail) so the server stays authoritative.

## Search

| Method | Path                     | Purpose                     | Status |
| ------ | ------------------------ | --------------------------- | ------ |
| GET    | `/search?q=…`            | Full-text across chats      | TBD    |
| GET    | `/saved-searches`        | List saved searches         | TBD    |
| POST   | `/saved-searches`        | Save one                    | TBD    |
| DELETE | `/saved-searches/{id}`   | Delete one                  | TBD    |

## Attachments

| Method | Path                              | Purpose                      | Status |
| ------ | --------------------------------- | ---------------------------- | ------ |
| POST   | `/attachments`                    | Multipart upload → uploadId  | TBD    |
| GET    | `/attachments/{id}`               | Download bytes               | TBD    |

Blocked by the "shared files & links" roadmap item (server does not yet
persist attachment bytes; `blob_ref` is null).

## Billing

Mobile does not run Stripe Checkout itself. It gets a portal URL from:

| Method | Path                        | Purpose                              | Status |
| ------ | --------------------------- | ------------------------------------ | ------ |
| GET    | `/billing/portal`           | One-shot portal URL (open in browser)| TBD    |
| GET    | `/billing/plan`             | Current plan + limits                | TBD    |

## Errors — proposed catalog

`INVALID_REQUEST`, `INVALID_CREDENTIALS`, `EMAIL_NOT_VERIFIED`,
`UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `PLAN_LIMIT_EXCEEDED`,
`NO_CONNECTED_MAILBOX`, `MAILBOX_ERROR`, `IDEMPOTENCY_MISMATCH`,
`RATE_LIMITED`, `INTERNAL_ERROR`.

## Server authority — non-negotiables

- The server assigns all IDs, computes `conversationKey`, decides
  `unreadCount`, chooses `deliveryState`, and applies plan limits. The
  client never sends any of these.
- Optimistic UI on the client is fine for outbound bubble rendering, but on
  reconciliation the server response wins — replace, don't merge.
- The mobile client MUST NOT bypass this contract by parsing website HTML.
