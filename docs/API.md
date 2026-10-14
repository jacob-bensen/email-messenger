# API contract

**Status (2026-10-14): no mobile REST API is implemented.** This file is the canonical contract;
the "Implemented" section is generated from the code, the "Draft v1" section is a proposal only
(nothing implements it — do not build the client against it until it is moved to Implemented).

## Implemented (non-HTML) endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous poll of the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` SSE; one event per `NewMailEvent`, 25 s keepalive. (Event payload shape: TBD — see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe JSON | `200`; `400` on bad signature |

Errors: HTML error page via `GlobalExceptionHandler`/`error.html`; no JSON error envelope exists (TBD).

## Draft v1 (NOT IMPLEMENTED) — what a mobile client needs
Base `/api/v1`, JSON, bearer-token auth (new `POST /api/v1/auth/login` → access+refresh token;
Google sign-in via ID-token exchange). Resources mirror the existing service layer:
- `GET /api/v1/chats?q=&mailbox=&filter=&page=` → page of `ConversationListItem`
  (`key,title,initials,group,memberCount,preview,lastActivity(epoch ms),unread,threadCount,accounts[],senderEmail`)
- `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[]` of `BubbleMessage`:
  `messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId`); marks read
- `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` (attachments multipart) → created message
- `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/{id}/sync`
- `GET /api/v1/chats/events` SSE (same as web)
- Errors: `{ "error": "<code>", "message": "..." }` with 400/401/403/404/409/429 (plan limit → 403 `plan_limit_exceeded`).
All of it must delegate to `ChatService` / `ConversationListService` / `OutboundMessageService` /
`MailAccountService` — never duplicate controller logic.
