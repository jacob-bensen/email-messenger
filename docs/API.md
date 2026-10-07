# API contract

**Status (2026-10-07): there is no mobile REST API yet.** This file is the canonical
contract that both repos follow. §1 is what exists today; §2 is the *proposed* `/api/v1`
contract for the mobile client — **PROPOSED, NOT IMPLEMENTED**. The first feature run
must implement §2.1–2.3 on the server and the client together, updating this file first.

## 1. Existing non-HTML endpoints (implemented)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | Polls the user's active mailboxes now. Response `200 {"imported": <int>}`. |
| GET | `/chats/events` | session | SSE stream (`text/event-stream`) of new-mail events (`ChatEventStream`). |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe events. |
| GET | `/actuator/health` | public | Health. |

Everything else is server-rendered HTML (see `WEB.md`).

## 2. Proposed `/api/v1` (mobile)

Conventions: JSON, UTF-8, timestamps as epoch milliseconds (UTC) named `*At`, ids as
strings for keys / numbers for DB ids, errors as
`{"error": {"code": "<SNAKE_CASE>", "message": "<text>"}}` with HTTP status
400 `VALIDATION_FAILED`, 401 `UNAUTHENTICATED`, 403 `PLAN_LIMIT_EXCEEDED` / `FORBIDDEN`,
404 `NOT_FOUND`, 429 `RATE_LIMITED`. The server is authoritative; the client never sends
computed values (unread state, plan limits, conversation keys).

### 2.1 Auth (TBD design — needs a decision)
Session cookie + CSRF does not suit a mobile client. Proposed: `POST /api/v1/auth/login`
`{email,password}` → `{accessToken, refreshToken, expiresAt}`; `POST /api/v1/auth/refresh`;
`POST /api/v1/auth/logout`; bearer token on all other calls, CSRF not required for bearer
requests. Reuses `UserService`, `LoginThrottleService`, `AuthEventService`. Google sign-in
for mobile: **TBD**.

### 2.2 Conversations
`GET /api/v1/chats?q=&mailbox=&filter=all|unread|…&page=0` →
```json
{ "items": [ { "key": "…", "title": "…", "initials": "AB", "group": false,
  "memberCount": 1, "preview": "…", "lastActivityAt": 1760000000000, "unread": true,
  "threadCount": 3, "accounts": ["me@x.com"], "senderEmail": "ada@y.com" } ],
  "page": 0, "hasMore": false, "counts": { "…": 0 } }
```
Shape mirrors `ConversationListItem`. Filter names = `ConversationListService.ChatFilter`.

`GET /api/v1/chats/{key}?mailbox=` → mirrors `ChatConversation`: `{key,title,initials,
members:[{email,displayName,initials,label,signatureHtml}], threadCount,
runs:[{sender,outbound,messages:[{messageId,bodyHtml,sentAt,outbound,replyToMessageId,
attachments:[{filename,mimeType,size}]}]}]}`. Marks the conversation read (as the web does).

### 2.3 Send
`POST /api/v1/chats/{key}/messages` `{ "body": "…", "subject": null, "asReply": false,
"mailbox": null }` → `201` the created `BubbleMessage`. Same rules as
`POST /chats/{key}/send`: body ≤ 100 000 chars, empty body only allowed with attachments,
signature appended server-side, plan limits enforced. Attachments: multipart variant **TBD**.

### 2.4 Later
Mailboxes list/sync, push-token registration, saved searches, account/billing summary: **TBD**.
