# API contract (canonical for backend + mobile client)

## Status: NO JSON API IMPLEMENTED
As of 2026-11-06 the server exposes only the HTML/form routes in `WEB.md`, plus:
- `POST /billing/webhook` — Stripe webhook (server-to-server, not for the client).
- `GET /chats/events` — SSE stream (session-cookie auth) of new-mail signals.
- `POST /chats/refresh` — session auth + CSRF; returns `{"imported": <int>}`.

The mobile client **cannot** be built against the server today: there is no token auth and
no `/api/**` surface. Below is a PROPOSAL for the first milestone — not implemented; the
next feature run should finalize it here before coding either side.

## PROPOSED v1 (not implemented)
Auth: `POST /api/v1/auth/login {email,password}` → `{accessToken, refreshToken, expiresIn}`;
`Authorization: Bearer`. Errors: `{ "error": {"code": string, "message": string} }`, 401/403/404/409/422/429.

| Method | Path | Backed by existing service |
|---|---|---|
| GET | `/api/v1/me` | `UserService`, plan via `PlanLimitService` |
| GET | `/api/v1/chats?q&mailbox&filter&page` → `ConversationListItem[]` + counts | `ConversationListService` |
| GET | `/api/v1/chats/{key}` → `ChatConversation` | `ConversationService`/`ChatService` |
| POST | `/api/v1/chats/{key}/send` | `ReplyService`/`OutboundMessageService` |
| GET | `/api/v1/chats/events` (SSE) | `ChatEventStream` |

Field shapes should be derived from the records `ConversationListItem`, `ChatConversation`,
`BubbleMessage` (see `service/`). Exact JSON schemas: TBD.
