# API contract (canonical — shared by backend and mobile client)

Status: **v0 — no mobile REST API is implemented yet.** This file documents
(1) the JSON/streaming endpoints that exist today and (2) the planned `/api/v1`
surface, marked **PLANNED** (not implemented; the client must not call it yet).
Anything not implemented is TBD, not guaranteed.

## 1. Existing non-HTML endpoints (implemented)

All use session-cookie auth unless noted. State-changing requests need the CSRF
token (`_csrf` / `X-CSRF-TOKEN`), which makes them awkward for a native client —
the reason for the planned `/api/v1` below.

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | `text/event-stream` (SSE); emits new-mail signals (`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature | Stripe → server only; not for clients |
| GET | `/actuator/health` | public | Spring Actuator health |
| POST | `/login` | public, form-encoded `email`,`password` (+CSRF) | 302; sets `JSESSIONID` |

## 2. PLANNED `/api/v1` (design target; first slice = read-only chats)

Conventions: JSON, UTF-8; timestamps as UTC epoch millis (`...EpochMilli`,
matching the existing view models); errors:
`{"error": "<code>", "message": "<human text>"}` with HTTP 400/401/403/404/409/429.
Auth: TBD (bearer token issued by `POST /api/v1/auth/token`; must not require CSRF).
All handlers must delegate to the same services as the Thymeleaf controllers.

| Method | Path | Notes | Backing service |
|---|---|---|---|
| POST | `/api/v1/auth/token` | `{email,password}` → `{accessToken,expiresAt}` | `UserService` / auth |
| GET | `/api/v1/chats?page=&q=&mailbox=&filter=` | `filter` ∈ `ALL,UNREAD,AWAITING,ATTACHMENTS`; page size 30 | `ConversationListService` |
| GET | `/api/v1/chats/{key}?mailbox=` | marks read; returns conversation | `ChatService` |
| POST | `/api/v1/chats/{key}/messages` | `{body, subject?, asReply?}` | (extracted send service) |
| GET | `/api/v1/mailboxes` | connected accounts | `MailAccountService` |

Planned DTOs (mirror existing records):
- `ChatListItem` = `ConversationListItem`: `key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail`
- `ChatCounts`: `all,unread,awaiting,attachments`
- `Chat` = `ChatConversation`: `key,title,initials,members[ChatMember],threadCount,runs[]`
- `ChatMember`: `email,displayName,initials,label,signatureHtml`
- `Run` = `SenderBubbleRun`: `sender,thread,outbound,messages[Bubble]`
- `Bubble` = `BubbleMessage`: `messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId`

`bodyHtml` is server-sanitized HTML; the client must render it in a restricted
view and never trust it as executable.

Error codes (planned): `unauthorized`, `forbidden`, `not_found`,
`validation_failed`, `plan_limit_exceeded`, `rate_limited`.
