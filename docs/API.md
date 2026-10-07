# API.md — canonical contract (backend ↔ mobile)

Status: **no mobile REST API exists yet.** This file documents (1) what is implemented and (2) the planned `/api/v1` surface, marked PLANNED. PLANNED endpoints must not be called by the client until implemented and marked IMPLEMENTED here. The server is authoritative; contract changes require updating this file and both repos in the same run.

## 1. Implemented endpoints usable programmatically (session cookie + CSRF)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` |
| GET | `/chats/events` | session | none | `text/event-stream`; events are `NewMailEvent` (payload shape TBD — see `ChatEventStream`) |
| POST | `/login` | public + CSRF | form `email`, `password`, `remember-me` | 302 (HTML); not mobile-suitable |

All other routes return HTML or redirects (see `docs/WEB.md`).

## 2. PLANNED `/api/v1` (not implemented)
Auth: bearer access token (short-lived) + refresh token; no cookies/CSRF. Errors: `{"error": "<code>", "message": "..."}` with 400 validation, 401 unauthenticated, 403 plan limit (`PlanLimitKind`), 404, 409.
- POST `/api/v1/auth/token` `{email,password}` → `{accessToken,refreshToken,expiresIn}`
- POST `/api/v1/auth/refresh`
- GET `/api/v1/chats?mailbox=&q=&filter=&page=` → page of `ConversationListItem` (key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail)
- GET `/api/v1/chats/{key}` → `ChatConversation`; marks read
- POST `/api/v1/chats/{key}/send` `{body,subject?,asReply?}` (attachments TBD)
- GET `/api/v1/mailboxes`
- GET `/api/v1/events` (SSE) or push (Web Push roadmap item)
These shapes are provisional drafts derived from existing view models; finalize before implementing.
