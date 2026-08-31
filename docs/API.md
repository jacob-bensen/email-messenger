# API — mobile / non-browser JSON contract (v1)

**Status:** DRAFT / not yet implemented. This file is the shared contract
between the backend and the ConexusMail mobile client. It is normative for
both sides — the client MUST match the shapes here, and the server MUST NOT
diverge without updating this file in the same commit.

**Base URL:** `/api/v1`
**Content type:** `application/json; charset=utf-8` for all requests and
responses (except SSE endpoints, which are `text/event-stream`).
**Time format:** ISO-8601 UTC with millisecond precision
(`2026-08-31T14:07:12.345Z`).
**Casing:** JSON keys are `camelCase`.

## Today

The backend currently exposes **only** these JSON endpoints; they are what
the mobile client can rely on right now:

| Method | Path | Notes |
|---|---|---|
| `POST` | `/chats/refresh` | Session-authenticated. Body: none. Returns `{ "imported": <int> }`. |
| `POST` | `/billing/webhook` | Stripe → server. Not for clients. |
| `GET` | `/chats/events` | SSE. Session-authenticated. Emits `new-mail` events. |

Everything below in this document is DESIGN — not yet built. When the
backend implements an endpoint here, remove its TBD marker in the same
commit that adds the server + client + tests.

---

## Auth (TBD — decision required)

The website uses server-side sessions via a `JSESSIONID` cookie. Two
viable options for the mobile client:

1. **Session cookie** — the mobile app POSTs to a JSON login endpoint,
   receives a `Set-Cookie: JSESSIONID=…`, and stores it in the platform
   cookie jar. Reuses `SecurityConfig` unchanged. Downside: refresh /
   logout / CSRF need care and cookie handling is per-platform-plugin in
   Flutter.

2. **Bearer tokens** — new `/api/v1/auth/login` returns
   `{ accessToken, refreshToken, expiresIn }`; every request sends
   `Authorization: Bearer …`. Cleaner for mobile, but adds a token store,
   refresh flow, and rotation logic on the server.

**Recommendation:** start with bearer tokens (option 2). Session cookies
tangle with the site's CSRF filter and can't easily coexist with
long-lived refresh. Owner decision needed before implementation.

### Endpoints (once auth is chosen)

```
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/register             # optional; can defer to web
POST   /api/v1/auth/password/forgot      # optional; can defer to web
GET    /api/v1/auth/me                   # returns UserSummary
```

`UserSummary`:
```
{
  "id": 123,
  "email": "you@example.com",
  "displayName": "Jane Q",
  "plan": "PRO",              // FREE | PRO | BUSINESS
  "emailVerified": true,
  "trialing": false,
  "trialEndsAt": null
}
```

---

## Mailboxes

```
GET    /api/v1/mailboxes                 # list connected accounts
POST   /api/v1/mailboxes                 # connect IMAP-password mailbox
POST   /api/v1/mailboxes/{id}/sync       # trigger a foreground poll
POST   /api/v1/mailboxes/{id}/signature  # update signature
DELETE /api/v1/mailboxes/{id}            # disconnect
```

`Mailbox`:
```
{
  "id": 42,
  "provider": "IMAP" | "GMAIL_OAUTH",
  "address": "you@example.com",
  "displayName": "Personal",
  "connectedAt": "2026-07-01T09:12:00.000Z",
  "lastSyncAt": "2026-08-31T14:07:12.345Z",
  "signature": "— Jane\nPhone: …",
  "needsReconnect": false          // true for expired Gmail OAuth
}
```

Gmail OAuth **cannot** be started from the mobile app in v1: the OAuth
consent screen redirect must happen in a browser. The mobile app opens
`/mailboxes/gmail/connect` in an in-app browser and polls
`GET /api/v1/mailboxes` after the redirect completes.

---

## Chats (conversations)

The unit is a **conversation** — one or more `EmailThread`s sharing a
participant set, keyed by `ConversationKeyService`.

```
GET    /api/v1/chats?page=0&size=30&filter=ALL&mailboxId=42&q=…
GET    /api/v1/chats/{key}
POST   /api/v1/chats/{key}/mark-read
POST   /api/v1/chats/{key}/send          # reply into thread or new email
POST   /api/v1/chats/new                 # brand-new conversation
GET    /api/v1/chats/{key}/pane          # not exposed — mobile assembles from /chats/{key}
GET    /api/v1/chats/events              # SSE — new-mail live stream
POST   /api/v1/chats/refresh             # already exists; migrated under /api/v1
```

Query params on `GET /api/v1/chats`:

- `page` (int, default 0)
- `size` (int, default 30, max 100)
- `filter` — `ALL` | `UNREAD` | `AWAITING` | `ATTACHMENTS` (matches
  `ConversationListService.ChatFilter`)
- `mailboxId` (long, optional — scope to one account; omit for the
  cross-account Hub)
- `q` (string, optional — free-text query)

`ChatListPage`:
```
{
  "page": 0,
  "size": 30,
  "total": 214,
  "counts": { "all": 214, "unread": 12, "awaiting": 4, "attachments": 61 },
  "items": [
    {
      "key": "b3f1…",
      "title": "Ada Lovelace",
      "initials": "AL",
      "isGroup": false,
      "memberCount": 1,
      "preview": "You: sounds good, see you then",
      "lastActivityAt": "2026-08-31T13:52:04.000Z",
      "unread": false,
      "threadCount": 3,
      "senderEmail": "ada@lovelace.co",
      "accountAddresses": ["you@example.com"]
    }
  ]
}
```

`Chat` (from `GET /api/v1/chats/{key}`):
```
{
  "key": "b3f1…",
  "title": "Ada Lovelace",
  "initials": "AL",
  "isGroup": false,
  "members": [
    {
      "email": "ada@lovelace.co",
      "displayName": "Ada Lovelace",
      "signature": "— Ada\nCEO, Analytical Engines Ltd.",
      "hasSignature": true
    }
  ],
  "threadCount": 3,
  "runs": [
    {
      "threadId": 8871,
      "side": "IN" | "OUT",
      "senderEmail": "ada@lovelace.co",
      "senderDisplayName": "Ada Lovelace",
      "sentAt": "2026-08-31T13:52:04.000Z",
      "messages": [
        {
          "id": 45211,
          "sentAt": "2026-08-31T13:52:04.000Z",
          "bodyHtml": "<p>Sounds good, see you then.</p>",
          "bodyText": "Sounds good, see you then.",
          "attachments": [
            { "id": 991, "filename": "notes.pdf", "mimeType": "application/pdf", "size": 18432, "downloadUrl": null }
          ]
        }
      ]
    }
  ]
}
```

`POST /api/v1/chats/{key}/send`:

Request:
```
{
  "body": "sounds good, see you then",
  "subject": null,                // optional; ignored when asReply=true
  "asReply": true,                // continue the newest thread (default) vs. start a new one
  "mailboxId": 42,                // optional; the account to send FROM
  "attachments": []               // optional; multipart upload path if used
}
```

Response `202 Accepted`:
```
{ "queued": true, "messageId": "<generated@server>" }
```

Attachments larger than a small inline threshold use
`multipart/form-data` instead of JSON — final scheme TBD.

`POST /api/v1/chats/new`:

Request:
```
{
  "to": ["ada@lovelace.co", "grace@hopper.mil"],
  "subject": "Lunch Friday?",
  "body": "Free at noon?",
  "mailboxId": 42,
  "attachments": []
}
```

Response `202 Accepted`:
```
{ "key": "b3f1…", "queued": true }
```

---

## Live stream

`GET /api/v1/chats/events` — Server-Sent Events, session- or bearer-
authenticated.

Event types:

- `new-mail` — `{ "conversationKey": "b3f1…", "count": 1 }` — the rail
  should refresh the affected row and the open pane if the key matches.
- `heartbeat` — periodic keepalive with no payload.

---

## Errors

All error responses share this shape:

```
{
  "error": {
    "code": "PLAN_LIMIT_EXCEEDED",
    "message": "You've reached the mailbox limit for your plan.",
    "detail": { "kind": "MAILBOX_COUNT", "limit": 1, "current": 1 }
  }
}
```

Known codes (extend as endpoints land):

| HTTP | code | Meaning |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Request body / query params rejected. |
| 401 | `UNAUTHENTICATED` | Missing / invalid token or session. |
| 402 | `PAYMENT_REQUIRED` | Subscription past-due. |
| 403 | `FORBIDDEN` | Authenticated but not permitted. |
| 403 | `PLAN_LIMIT_EXCEEDED` | Blocked by `PlanLimitService`. |
| 404 | `NOT_FOUND` | Chat / mailbox / attachment doesn't exist. |
| 409 | `EMAIL_ALREADY_REGISTERED` | Registration conflict. |
| 409 | `MAILBOX_NEEDS_RECONNECT` | Gmail OAuth token expired. |
| 429 | `RATE_LIMITED` | Login-throttle / general limits. |
| 500 | `INTERNAL` | Unexpected server error. |

---

## Contract-first workflow

Every mobile-facing feature MUST:

1. Update this file with the endpoint(s), request/response shapes, and
   error codes.
2. Implement the server side calling the SAME `@Service` beans the
   Thymeleaf website uses (see `docs/WEB.md` → "Service layer used by
   the pages"). Never fork business logic into a JSON controller.
3. Implement the mobile side with DTOs that match the shapes here byte
   for byte.
4. Add both server and client tests in the same PR.
