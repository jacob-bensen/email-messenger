# API contract

Canonical contract between the backend and the mobile client (`conexus-mail-mobile`).

**Status (2026-10-21): no mobile REST API exists.** Only the endpoints below
return non-HTML. Everything under "Proposed v1" is a *design target, not
implemented* — implement it here first (contract-first) before either repo
codes against it.

## Implemented non-HTML endpoints

All require a session cookie unless noted; mutating calls require the CSRF token.

### `POST /chats/refresh`
Polls the caller's mailboxes now. `200 {"imported": <int>}`.

### `GET /chats/events` (SSE, `text/event-stream`)
Event `new-mail`, data `{"imported": <int>}`. Comment `ping` as keep-alive.

### `POST /billing/webhook` (public, Stripe-signed, CSRF-exempt)
Stripe webhook. Not for the client.

## Proposed v1 (TBD — not implemented)

Auth: `POST /api/v1/auth/token` (email+password → bearer token), refresh and
revoke; bearer filter on `/api/v1/**`, CSRF-exempt. Errors:
`{"error": "<code>", "message": "<text>"}` with 401/403/404/409/422/429.

| Endpoint | Backed by (existing service/record) |
|---|---|
| `GET /api/v1/chats` | `ConversationListService` → `ConversationListItem` (key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail) |
| `GET /api/v1/chats/{key}` | `ChatService` → `ChatConversation` / `BubbleMessage` (messageId,bodyHtml,sentAt,attachments,outbound,replyToMessageId) |
| `POST /api/v1/chats/{key}/messages` | `ReplyService` / `OutboundMessageService` |
| `GET /api/v1/mailboxes` | `MailboxView` |
| `GET /api/v1/events` | SSE, as `/chats/events` |

Shapes are indicative; finalize here before coding.
