# API.md — canonical API contract (client ⇄ server)

**Status (2026-10-05): the server exposes NO REST API.** The website is server-rendered
(session cookie + CSRF + HTML forms, see `WEB.md`), which a native client cannot use
sanely. The mobile client (`conexus-mail-mobile`) is an empty scaffold and calls nothing.

This file is the contract both repos build against. Sections marked **PROPOSED** are
not implemented on either side; they become binding once implemented and marked
**IMPLEMENTED** in the same commit.

## Existing non-HTML endpoints (IMPLEMENTED)
- `GET /chats/events` — `text/event-stream`, session-authenticated; emits new-mail signals.
- `POST /billing/webhook` — Stripe, signature-verified, CSRF-exempt. Not for clients.

## v1 conventions (PROPOSED)
- Base path `/api/v1`, JSON, UTF-8. Timestamps: epoch millis UTC (matches
  `ConversationListItem.lastActivityEpochMilli`).
- Auth: bearer access token (short-lived) + refresh token; stateless, no CSRF. TBD: token
  storage table (new Flyway migration) and Google sign-in via ID-token exchange.
- Errors: `{ "error": "<CODE>", "message": "...", "details": {} }`; HTTP 400 validation,
  401 unauthenticated, 402/403 `PLAN_LIMIT_EXCEEDED` (maps `PlanLimitExceededException`),
  404, 409, 429.
- All handlers must call the existing services; no logic in controllers.

## v1 endpoints (PROPOSED, first slice)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | none | `{email,password}` | `{accessToken,refreshToken,expiresInSec,user:{id,email,plan}}` |
| POST | `/api/v1/auth/refresh` | none | `{refreshToken}` | same tokens |
| GET | `/api/v1/me` | bearer | – | `{id,email,plan,mailboxes:[{id,address,provider}]}` |
| GET | `/api/v1/chats?page&q&mailbox&filter` | bearer | – | `{items:[ConversationListItem],page,hasNext}` |
| GET | `/api/v1/chats/{key}` | bearer | – | `ChatConversation` |
| POST | `/api/v1/chats/{key}/send` | bearer | `{body,subject?,asReply,mailboxId?}` | `BubbleMessage` |

`ConversationListItem` = `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
(derived from `service/ConversationListItem`). `BubbleMessage` = `{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}`.
`ChatConversation` member/run shapes: TBD (see `ChatMember`, `SenderBubbleRun`).
