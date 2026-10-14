# API contract (canonical, shared with `conexus-mail-mobile`)

**Status: no mobile REST API is implemented yet.** Nothing below marked PROPOSED exists in code.
Per routine rules the contract is written *before* implementation; do not implement a PROPOSED endpoint
without first confirming/adjusting it here and updating the client in the same run.

## 1. Implemented, non-HTML endpoints (session-cookie auth + CSRF)

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream` of new-mail events (`NewMailEvent`); exact event names/payload TBD (read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe-internal |

## 2. PROPOSED `/api/v1` (v1 target: read-only chats + send)

Auth (PROPOSED, TBD decision): bearer access token issued by `POST /api/v1/auth/login`
(`{email,password}` → `{accessToken, expiresAt, refreshToken}`); `/api/v1/**` exempt from CSRF, stateless.
Errors: `{"error":{"code":"<SNAKE_CASE>","message":"..."}}` with 400 `VALIDATION`, 401 `UNAUTHENTICATED`, 403 `PLAN_LIMIT`/`FORBIDDEN`, 404 `NOT_FOUND`.

| Method | Path | Maps to service | Notes |
|---|---|---|---|
| GET | `/api/v1/me` | `UserService` | `{id,email,displayName,plan,emailVerified}` |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` | `[{id,address,authType,provider}]` |
| GET | `/api/v1/chats?page&q&mailbox&filter=all\|unread\|awaiting\|attachments` | `ConversationListService.list/counts` | `{items:[ConversationListItem],counts:{all,unread,awaiting,attachments},page,hasNext}`; item = `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}` |
| GET | `/api/v1/chats/{key}?mailbox` | `ChatService` (+markRead) | `ChatConversation`: `{key,title,initials,members[],threadCount,runs:[{…,messages:[{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}]}`; run/member shapes TBD from `SenderBubbleRun`/`ChatMember` |
| POST | `/api/v1/chats/{key}/send` | `ReplyService`/`OutboundMessageService` | `{body,subject?,asReply?}` (attachments TBD) |
| POST | `/api/v1/chats/refresh` | `MailboxPollingService.refreshActiveUserNow` | `{imported}` |

Field shapes mirror the existing records in `com.emailmessenger.service` (single source for web + API).
