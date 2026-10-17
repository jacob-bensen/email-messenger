# API.md — canonical API contract

Status 2026-10-17: **the backend exposes no mobile REST API yet.** §1 documents what exists
(verified from source). §2 is the *proposed* `/api/v1` contract for the mobile client — marked
PLANNED; nothing in §2 is implemented. Implement §2 only after resolving the decisions in
INTERNAL_TODO (auth model). Update this file before changing any endpoint.

## 1. Existing non-HTML endpoints (implemented)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` (`SseEmitter`); emits new-mail events (`NewMailEvent`) per user |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | raw Stripe JSON | `200` / signature error |
| GET | `/actuator/health[/**]` | public | — | Spring health |

Errors: handled by `GlobalExceptionHandler` (HTML error page; `IllegalArgumentException` → 400; oversized upload → 413). No JSON error envelope exists.

All other routes are HTML form endpoints (see `WEB.md`). They are **not** a stable client contract.

## 2. PLANNED `/api/v1` (not implemented) — shapes derived from existing view-model records

Auth: TBD (decision needed — bearer token issued by `POST /api/v1/auth/token` vs. session). CSRF must not apply to bearer calls.
Error envelope (proposed): `{"error":{"code":"string","message":"string"}}` with 400/401/403/404/409/413/422.

| Method | Path | Maps to | Response / notes |
|---|---|---|---|
| GET | `/api/v1/chats?q&mailbox&filter=all|unread|...&page` | `ConversationListService.list/counts` | `{items:[ConversationListItem], counts, page}` |
| GET | `/api/v1/chats/{key}` | `ChatService.markRead` + chat view | `ChatConversation` |
| POST | `/api/v1/chats/{key}/messages` | `ReplyService` + `OutboundMessageService` | body `{body, subject?, asReply?}` (+ attachments multipart) |
| POST | `/api/v1/chats` | new conversation | `{to:[email], subject?, body}` |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` | list (never returns credentials) |

DTO shapes (from `service/` records; times are UTC epoch millis in JSON):
- `ConversationListItem {key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts[],senderEmail}`
- `ChatConversation {key,title,initials,members[ChatMember],threadCount,runs[SenderBubbleRun]}`
- `ChatMember {email,displayName,initials,label,signatureHtml}`
- `BubbleMessage {messageId,bodyHtml,sentAt,attachments[],outbound,replyToMessageId}`
