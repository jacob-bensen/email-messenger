# API.md — canonical API contract

**Status (2026-10-30): there is NO mobile REST API.** The server is session-cookie +
CSRF + HTML. The mobile client (`conexus-mail-mobile`) currently has no code. Anything
below under "Planned" is a proposal (TBD), not implemented behavior.

## Implemented non-HTML endpoints
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | `text/event-stream`; `NewMailEvent` pushes (ChatEventStream) |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | Stripe events |
| GET | `/actuator/health` | public | Spring health |

## Planned (TBD — define before implementing)
First slice proposal, all under `/api/v1`, JSON, `Authorization: Bearer <token>`:
1. `POST /api/v1/auth/token` — email+password → token.
2. `GET /api/v1/chats?mailbox=&q=&filter=&page=` → page of `ConversationListItem`
   (`key,title,initials,group,memberCount,preview,lastActivity(epoch ms),unread,threadCount,accounts[],senderEmail`).
3. `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[]`).
4. `POST /api/v1/chats/{key}/send`.
Errors: `{ "error": "<code>", "message": "…" }` with 400/401/403/404/409.
All must call the existing `ConversationListService`/`ChatService`/`ReplyService`.
