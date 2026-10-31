# API contract

**Status (2026-10-31): no mobile REST API exists yet.** This file is the canonical
contract. §1 documents what the server really exposes today; §2 is the *planned*
`/api/v1` surface (design only — nothing in §2 is implemented, and the mobile repo must
not call it until it moves to §1). Unknowns are marked TBD.

## 1. Implemented machine-readable endpoints

All require an authenticated browser session (cookie `JSESSIONID`) unless noted. Errors
for unauthenticated requests are a `302` to `/login`, not JSON. Unknown/foreign ids
surface as the HTML error page with status `404`; `MailException`/import failures `502`.

### `POST /chats/refresh`
Synchronously polls the user's connected mailboxes. CSRF token required.
- 200 `application/json`: `{"imported": <int>}` — newly imported messages.

### `GET /chats/events`
Server-Sent Events stream (`text/event-stream`), one emitter per tab.
- Event `new-mail`, data `{"imported": <int>}`, published after a poll imports mail.
- Keep-alive: SSE comment `ping` periodically.

### `POST /billing/webhook`
Stripe webhook. No session, CSRF-exempt, authenticated by Stripe signature header.
Not for clients.

Everything else (`/chats`, `/chats/{key}`, `/mailboxes/**`, …) returns HTML or redirects;
see `WEB.md`. They are form endpoints, not part of the client contract.

## 2. Planned `/api/v1` (TBD — not implemented)

Design constraints decided so far:
- Server authoritative; client `lib/domain/` only mirrors rules (plan limits, conversation
  grouping rules are computed server-side and returned).
- Needs **bearer-token auth** (no mechanism exists; session+CSRF is unsuitable for mobile).
  Mechanism TBD (short-lived access token + rotating refresh token recommended).
- JSON DTOs should be thin projections of existing view-model records:
  `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts,senderEmail`),
  `ChatConversation` (`key,title,initials,members[],threadCount,runs[]`),
  `BubbleMessage` (`messageId,bodyHtml,sentAtEpochMilli,attachments,outbound,replyToMessageId`).
- Proposed first slice: `POST /api/v1/auth/token`, `GET /api/v1/chats?page&q&filter&mailbox`,
  `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`.
- Prerequisite refactor: move send orchestration out of `ConversationController` into a
  service (see `BACKEND.md`).
