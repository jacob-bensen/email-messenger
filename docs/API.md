# API — canonical contract between server and mobile client

**Status (2026-10-06): no mobile REST API is implemented.** The server only
exposes the HTML website (see `WEB.md`). This file defines the *planned* v1
contract; everything below is **PLANNED — not implemented** until the
INTERNAL_TODO items land. The Flutter repo (`conexus-mail-mobile`) is an empty
scaffold and depends on this file.

## Conventions (planned)
- Base path `/api/v1`, JSON, UTF-8, ISO-8601 UTC timestamps.
- Auth: `Authorization: Bearer <access token>`; tokens issued by
  `POST /api/v1/auth/login` (email + password) with refresh. Stateless, no CSRF.
- Errors: `{"error": {"code": "<SNAKE_CASE>", "message": "..."}}` with HTTP
  400 validation, 401 unauthenticated, 402 `PLAN_LIMIT_EXCEEDED`, 403, 404, 413, 429.
- Server authoritative: plan limits (`PlanLimits`), thread caps, mailbox caps.

## Planned endpoints (v1 minimum for a chat client)
| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/login`, `/auth/refresh`, `/auth/logout` | token auth |
| GET | `/api/v1/chats?cursor=` | list conversations (`ConversationListService`) |
| GET | `/api/v1/chats/{key}?before=` | bubble runs (`ChatService`) |
| POST | `/api/v1/chats/{key}/messages` | send reply (`OutboundMessageService`) |
| GET | `/api/v1/mailboxes` | connected accounts |
| GET | `/api/v1/me` | user + plan + limits |
| GET | `/chats/events` (SSE) | exists for web; reuse with bearer auth |

Request/response schemas: TBD — define alongside the first implemented endpoint.
