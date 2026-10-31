# API contract (canonical — shared with `conexus-mail-mobile`)

Status legend: **IMPLEMENTED** = exists in code today; **PROPOSED** = agreed target,
no server code yet. Do not code a client against PROPOSED rows until they flip to
IMPLEMENTED in the same commit as the server work.

## 1. Implemented, JSON/stream endpoints (session-cookie auth + CSRF)

### POST /chats/refresh — IMPLEMENTED
Auth: session. Header `X-CSRF-TOKEN` (or `_csrf`) required.
Response 200: `{"imported": <int>}`. Errors: 302→/login if unauthenticated, 403 CSRF.

### GET /chats/events — IMPLEMENTED
Auth: session. `text/event-stream`; per-user live new-mail events (`NewMailEvent`).
Event payload schema: TBD (read `ChatEventStream` before relying on it).

## 2. Mobile REST API v1 — PROPOSED (nothing implemented)

Base `/api/v1`, JSON, UTF-8. Auth: `Authorization: Bearer <access token>`; no session
or CSRF. Timestamps are epoch milliseconds (UTC), matching existing view records.
Error body: `{"error": "<code>", "message": "<text>"}`; codes: `unauthorized`(401),
`forbidden`(403), `not_found`(404), `validation`(400), `plan_limit`(402 — carries
`kind`: THREAD_COUNT|MAILBOX_COUNT|SAVED_SEARCH_COUNT), `conflict`(409).

| Method & path | Purpose | Backed by (must reuse) |
|---|---|---|
| POST `/auth/token` `{email,password}` → `{accessToken,refreshToken,expiresIn}` | login | `UserService` |
| POST `/auth/refresh` | rotate token | TBD |
| GET `/chats?mailbox&q&filter&page` → `{items:[ConversationListItem],counts}` | list | `ConversationListService` |
| GET `/chats/{key}?mailbox` → `ChatConversation` (marks read) | open | `ChatService` |
| POST `/chats/{key}/messages` `{body, subject?, asReply?}` → 201 `BubbleMessage` | send | **new** `ChatSendService` extracted from `ConversationController.send` |
| GET `/mailboxes` | accounts | `MailAccountService` |

DTO shapes (mirror existing records, field names unchanged):
- `ConversationListItem`: `key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail`
- `ChatConversation`: `key,title,initials,members[ChatMember],threadCount,runs[BubbleRun]`
- `BubbleRun`: `sender,messages[BubbleMessage],outbound`
- `BubbleMessage`: `messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId`
(`ChatMember`, `SenderView`, `Attachment` field lists: TBD — fill in when implementing.)

Open decisions: token format (opaque DB-backed vs JWT), whether `bodyHtml` is served
sanitized-HTML or markdown, attachment download (blocked on blob store, see CLAUDE.md roadmap).
