# API contract (canonical)

Status legend: **IMPLEMENTED** = exists in code today; **PROPOSED** = agreed target, not implemented. Clients must only
call IMPLEMENTED endpoints. Last audited: 2026-10-03 (Phase 0).

## 1. Current reality
The backend exposes **no REST API** for the mobile client. Everything is session-cookie + CSRF + HTML, except:

### POST /chats/refresh — IMPLEMENTED
Auth: session cookie + CSRF token. Synchronously polls the user's mailboxes.
Response `200`: `{"imported": <int>}`. Unauthenticated → `302 /login`.

### GET /chats/events — IMPLEMENTED
Auth: session cookie. `text/event-stream`; one SSE emitter per user (`ChatEventStream`), fired on `NewMailEvent(userId, importedCount)`.
Event name/payload shape: TBD (read `ChatEventStream` before relying on it).

### POST /billing/webhook — IMPLEMENTED
Stripe only (signature header, CSRF-exempt). Not for clients.

## 2. PROPOSED v1 (mobile) — `/api/v1`
Not implemented. Implementing it is INTERNAL_TODO #1. Shapes mirror existing view records
(`ConversationListItem`, `ChatConversation`, `BubbleRun`, `BubbleMessage`, `ChatMember`).

Conventions: JSON, UTF-8, timestamps are UTC epoch millis (`*EpochMilli`, as in existing view models).
Auth: `Authorization: Bearer <accessToken>` (mechanism TBD: short-lived token from `POST /api/v1/auth/login`, CSRF not applicable).
Errors: `{"error": {"code": "<UPPER_SNAKE>", "message": "<text>"}}` with codes `UNAUTHENTICATED(401)`, `FORBIDDEN(403)`,
`NOT_FOUND(404)`, `VALIDATION(422)`, `PLAN_LIMIT(402)`, `PAYLOAD_TOO_LARGE(413)`.

| Method & path | Purpose | Request | Response |
|---|---|---|---|
| `POST /api/v1/auth/login` | email+password → token | `{email,password}` | `{accessToken, expiresAt, user:{id,email,plan}}` |
| `GET /api/v1/chats` | conversation list | query `page,q,mailbox,filter` | `{items:[ConversationListItem], counts:{all,unread,awaiting,attachments}, page, hasNext}` |
| `GET /api/v1/chats/{key}` | open conversation (marks read) | query `mailbox` | `{key,title,initials,group,members:[ChatMember],threadCount,runs:[BubbleRun]}` |
| `POST /api/v1/chats/{key}/messages` | send | `{body, subject?, asReply?}` (+attachments TBD) | `201` BubbleMessage |
| `POST /api/v1/chats` | new conversation | `{to:[email], subject?, body, mailbox?}` | `201 {key}` |
| `POST /api/v1/chats/refresh` | poll now | – | `{imported}` |

`ConversationListItem` JSON: `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts:[string],senderEmail}`.
`BubbleMessage`: `{messageId,bodyHtml,sentAtEpochMilli,attachments,outbound,replyToMessageId}`. `bodyHtml` is pre-sanitized server HTML.
