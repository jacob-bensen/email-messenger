# API.md — canonical contract between backend and mobile client

**Status (2026-10-31): NO JSON API IS IMPLEMENTED.** The server exposes only the
Thymeleaf website, one SSE stream, and the Stripe webhook (see `WEB.md`). The mobile
client therefore has nothing to call. This file defines the contract that the first
feature run will implement; sections marked PLANNED are not yet real.
Rule: update this file *before* changing either implementation.

## Conventions (PLANNED)
- Base path `/api/v1`, JSON (`application/json`), UTF-8, ISO-8601 UTC timestamps.
- Auth: `Authorization: Bearer <accessToken>`; tokens issued by `POST /api/v1/auth/login`.
  API paths are exempt from CSRF and cookie sessions (stateless filter chain).
- Errors: `{ "error": "<CODE>", "message": "<human text>" }` with HTTP 400/401/403/404/409/429.
- Server is authoritative; client never sends computed values (counts, plan limits, conversationKey).

## Implemented non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE; emits new-mail events (`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe events |
| GET | `/actuator/health` | public | health |

## PLANNED v1 endpoints (not implemented)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/login` | `{email,password}` → `{accessToken,expiresIn,refreshToken}` |
| POST | `/api/v1/auth/refresh` | refresh token → new access token |
| GET | `/api/v1/me` | user, plan, limits |
| GET | `/api/v1/chats?page&q&mailbox&filter` | page of conversation list items (`key,title,initials,lastMessagePreview,lastAt,unread,...`) mirroring `ConversationListItem` |
| GET | `/api/v1/chats/{key}` | `ChatConversation` (members, runs of bubbles) |
| POST | `/api/v1/chats/{key}/send` | `{body,subject?,asReply}` → 202 |
| GET | `/api/v1/mailboxes` | connected accounts |

Exact schemas: TBD at implementation time (derive from `service/ChatConversation`, `ConversationListItem`, `BubbleRun`).
