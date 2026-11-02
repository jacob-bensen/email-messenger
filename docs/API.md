# API.md — canonical contract (client ↔ server)

Status: **no mobile JSON API exists yet.** This file records what is implemented today and
the planned shape. Implementations must not diverge from it; update this file FIRST.

## 1. Implemented, machine-consumable endpoints today
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | — | `text/event-stream`; events published from `NewMailEvent` (payload schema TBD) |
| POST | `/billing/webhook` | Stripe signature header | raw Stripe JSON | 200 / 400 on bad signature |
| GET | `/actuator/health` | public | — | Spring Actuator health |

All other routes return HTML (see WEB.md). Errors: standard Spring (`/error` HTML page); no JSON error envelope yet.

## 2. Planned (TBD, not implemented) — `/api/v1`
Needed by the Flutter client. Auth: bearer access token (login with email+password → token pair);
CSRF not applicable to bearer routes. Proposed first slice:
- `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}`
- `GET /api/v1/chats?page&q&mailbox&filter` → page of `ConversationListItem`
- `GET /api/v1/chats/{key}` → `ChatConversation` (bubble runs)
- `POST /api/v1/chats/{key}/messages` `{body,subject?,asReply?}` → created message
- JSON error envelope `{"error":{"code","message"}}`
Each must delegate to the existing services (`ConversationListService`, `ChatService`, `ReplyService`).
