# API.md — canonical API contract

**Status (2026-10-04): no mobile/REST API is implemented.** The backend is a
session-cookie web app; the sibling repo `conexus-mail-mobile` is an empty
Flutter skeleton with no networking code. Nothing below may be assumed by the
client until it is moved from "Proposed" to "Implemented" in the same commit
as the code.

## Implemented non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature | Stripe events; CSRF-exempt |
| GET | `/chats/events` | session | `text/event-stream`, new-mail events (`NewMailEvent`) |
| POST | `/chats/refresh` | session + CSRF | `@ResponseBody`; triggers mailbox refresh. Response schema TBD |
| GET | `/actuator/health` | public | Spring actuator |

## Proposed (not implemented) — `/api/v1`
Auth: TBD (needs token auth; session+CSRF is unsuitable for mobile).
Error shape: `{ "error": "<code>", "message": "<text>" }`.

1. `POST /api/v1/auth/login` → `{ accessToken, refreshToken, expiresIn }`
2. `GET /api/v1/chats?page=&q=&mailbox=&filter=` → `{ items: ConversationListItem[], page, hasMore }`
   where `ConversationListItem` = `{ key, title, initials, group, memberCount, preview, lastActivityEpochMilli, unread, threadCount, accounts[], senderEmail }` (mirrors `service/ConversationListItem`).
3. `GET /api/v1/chats/{key}` → messages grouped in bubble runs (shape TBD from `ChatConversation`).
4. `POST /api/v1/chats/{key}/messages` → send reply via `ReplyService`.

Plan limits surface as HTTP 402 `{error:"plan_limit"}` (maps `PlanLimitExceededException`) — TBD.
