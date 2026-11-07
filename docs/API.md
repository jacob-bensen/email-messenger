# API.md — canonical API contract (mobile ↔ server)

**Status: DRAFT / NOT IMPLEMENTED.** The server currently exposes no REST API for the mobile client (see `INTERNAL_TODO.md`). Only the two JSON/stream endpoints below exist, and both are session-cookie + CSRF protected. Everything else is TBD and must be defined here BEFORE implementation.

## Existing machine-readable endpoints
| Method/Path | Auth | Response |
|---|---|---|
| `POST /chats/refresh` | session cookie + CSRF token | `200 {"imported": <int>}` |
| `GET /chats/events` | session cookie | `text/event-stream` of new-mail events (`ChatEventStream`, payload `NewMailEvent`; exact event names TBD) |

## Proposed v1 (TBD — to be finalized on the first feature run)
Base `/api/v1`, JSON, bearer-token auth (new; stateless filter chain separate from the web chain, CSRF-exempt). Candidates in priority order:
1. `POST /api/v1/auth/login` `{email,password}` → `{accessToken, refreshToken, expiresIn}`; `POST /api/v1/auth/refresh`
2. `GET /api/v1/conversations?mailbox&filter&q&page` → page of `ConversationListItem`
3. `GET /api/v1/conversations/{key}` → bubble runs (`ChatConversation`)
4. `POST /api/v1/conversations/{key}/messages` → send (via `OutboundMessageService`)
5. `GET /api/v1/mailboxes`, `POST /api/v1/conversations/refresh`

Error shape (proposed): `{ "error": "<code>", "message": "<text>" }`; 401 unauthenticated, 403 plan limit (`PlanLimitExceededException`), 404 unknown key, 422 validation.
