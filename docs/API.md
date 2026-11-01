# API — canonical contract (client ⇄ server)

**Status: DRAFT / NOT IMPLEMENTED.** As of 2026-11-01 the server exposes **no JSON API**; the only
JSON-ish endpoints are `POST /chats/refresh` (`{"imported":n}`, session+CSRF) and the Stripe webhook.
The sections below are the *proposed* v1 contract. Nothing here may be coded on either side until the
auth decision in INTERNAL_TODO #1 is made; then this file is finalised first, per the sync routine.

## Conventions (proposed)
- Base path `/api/v1`, JSON UTF-8, ISO-8601 UTC timestamps (server stores `LocalDateTime` as UTC).
- Auth: `Authorization: Bearer <accessToken>`. Errors: `{"error":{"code":"…","message":"…"}}` with
  HTTP 400/401/403/404/409/422/429. Plan-limit breach → 403 `PLAN_LIMIT` with `kind`
  (THREAD_COUNT|MAILBOX_COUNT|SAVED_SEARCH_COUNT).
- Server is authoritative: client never sends plan, unread, conversationKey for new chats, or
  sender address.

## Endpoints (proposed, in implementation order)
| # | Method & path | Purpose | Backed by existing service |
|---|---|---|---|
| 1 | `POST /auth/token` `{email,password}` → `{accessToken,refreshToken,expiresIn}` | login | `DbUserDetailsService`, throttle |
| 2 | `POST /auth/refresh` | rotate token | new |
| 3 | `GET /me` → `{id,email,displayName,plan,emailVerified}` | profile | `UserService`, `AccountBillingSummary` |
| 4 | `GET /mailboxes` → `[{id,username,host,authType,lastSyncedAt,lastSyncError}]` | | `MailAccountService` |
| 5 | `GET /chats?q&mailbox&filter&page` → `{items:[ConversationListItem],page,counts}` | list | `ConversationListService` |
| 6 | `GET /chats/{key}` → `ChatConversation` (marks read) | open | `ChatService` |
| 7 | `POST /chats/{key}/messages` `{body,subject?,asReply?}` → 201 `BubbleMessage` | send | `OutboundMessageService` |
| 8 | `GET /chats/events` (SSE) | new-mail push | `ChatEventStream` |
| 9 | `POST /chats/refresh` → `{imported}` | sync now | `MailboxPollingService` |

## DTO shapes (mirror existing records)
`ConversationListItem {key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
`ChatConversation {key,title,initials,members[{email,name,signatureHtml}],threadCount,runs[SenderBubbleRun]}`
`SenderBubbleRun {sender,thread{id,subject},messages[BubbleMessage],outbound}`
`BubbleMessage {messageId,bodyHtml,sentAtEpochMilli,attachments[{filename,mimeType,sizeBytes}],outbound,replyToMessageId}`
(`bodyHtml` is pre-sanitised server-side; client must render it in a sanitising view.)
