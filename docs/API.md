# API contract (canonical)

**Status (2026-11-03): no mobile REST API is implemented.** This file documents (1) what exists today that a client could technically call, and (2) the *proposed* v1 surface, marked PROPOSED. Nothing marked PROPOSED exists in code; do not implement client calls against it until the backend work is done in a feature run and this file is flipped to IMPLEMENTED.

## 1. Existing JSON / stream endpoints (session-cookie auth + CSRF)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session | none | `200 {"imported": <int>}` — synchronous poll of the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); "new mail" signal per user (event payload shape: TBD, see `ChatEventStream`) |

Errors: unauthenticated → 302 to `/login` (HTML, not 401); upload too large → 413 via `GlobalExceptionHandler`.

## 2. PROPOSED v1 mobile API (not implemented)
Auth: `POST /api/v1/auth/login` → bearer token (new; the current session+CSRF model is unsuitable for mobile). All endpoints under `/api/v1`, JSON, errors as `{"error": "<code>", "message": "..."}` with 401/403/404/409/422.

| Method | Path | Maps to existing logic |
|---|---|---|
| POST | `/api/v1/auth/login`, `/auth/register` | `UserService`, `AuthController` |
| GET | `/api/v1/chats?q=&filter=&mailbox=&page=` | `ConversationListService.list` → `ConversationListItem[]` + counts |
| GET | `/api/v1/chats/{key}?mailbox=` | `ChatService.buildFor` + `markRead` → `ChatConversation` |
| POST | `/api/v1/chats/{key}/messages` | `ReplyService` + `OutboundMessageService` (needs logic extracted from `ConversationController.send`) |
| POST | `/api/v1/chats` | new conversation (`ConversationController.createMessage`) |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` |
| GET | `/api/v1/events` | SSE, same as `ChatEventStream` |

DTO source shapes (existing Java records): `ConversationListItem(key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail)`; `ChatConversation(key,title,initials,members,threadCount,runs)`; `BubbleMessage(messageId,bodyHtml,sentAt,attachments,outbound,replyToMessageId)`. Final JSON field names/timestamps (epoch millis UTC, per `lastActivityEpochMilli`) TBD when the contract is promoted.
