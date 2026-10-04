# API.md — canonical API contract

## Status (2026-10-04): **no JSON API is implemented**

The backend exposes only server-rendered HTML (see `WEB.md`), one SSE stream and the Stripe
webhook. The mobile client (`conexus-mail-mobile`) has nothing to call yet. This file is the
shared source of truth: an endpoint is only "Implemented" once both repos match it.

## Conventions (decided for v1, applied when the first endpoint lands)
- Base path `/api/v1`, JSON (`application/json`), timestamps as UTC epoch millis
  (matches `ConversationListItem.lastActivityEpochMilli`).
- Auth: **TBD** — session cookies + CSRF exist today; mobile needs bearer tokens
  (token issue/refresh endpoints, stored hashed server-side). Decision pending.
- Errors: `{ "error": "<code>", "message": "<human text>" }` with 400/401/403/404/409/429.
- All business rules (plan limits, ownership checks) are enforced server-side via the
  existing service layer.

## Existing non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE; new-mail notifications (`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature | raw JSON, CSRF-exempt |

## Proposed v1 (NOT implemented; mirrors service records)
| Method | Path | Backed by | Response |
|---|---|---|---|
| POST | `/api/v1/auth/token` | `UserService` | token pair — TBD |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `ConversationListService` | page of `ConversationListItem` {key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail} |
| GET | `/api/v1/chats/{key}` | `ChatService` | `ChatConversation` {key,title,initials,members[],threadCount,runs[]} with `BubbleMessage` {messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId} |
| POST | `/api/v1/chats/{key}/messages` | `ReplyService`/`OutboundMessageService` | created message |
| GET | `/api/v1/mailboxes` | `MailAccountService` | accounts |
