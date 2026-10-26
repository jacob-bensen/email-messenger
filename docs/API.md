# API contract (canonical)

**Status 2026-10-26: no mobile REST API is implemented.** The mobile repo
(`conexus-mail-mobile`) is an empty Flutter placeholder. This file records (1) what the
server exposes today and (2) the *planned* v1 contract. Planned endpoints are NOT
implemented; do not treat them as live. Before implementing any, refine here first.

## 1. Existing machine-readable endpoints (session-cookie auth, CSRF required on POST)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — synchronous mailbox refresh |
| GET | `/chats/events` | session | `text/event-stream`; new-mail events (payload shape TBD — see `NewMailEvent`/`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe event ingestion |
| GET | `/actuator/health` | none | Boot health |

Errors today are HTML (`error.html`) via `GlobalExceptionHandler`: 404 (missing), 502
(mail/billing upstream failure), 413 (oversized upload). Unauthenticated requests get a
302 to `/login`.

## 2. PLANNED v1 (draft — TBD, unimplemented)
Principles: JSON, `/api/v1`, Bearer-token auth (new; separate from the session chain),
errors as `{"error":{"code":"...","message":"..."}}`, all handlers delegate to the same
services as the web controllers.

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/token` | `{email,password}` → `{accessToken,expiresAt}`; reuses login throttle |
| GET | `/api/v1/chats?page&q&filter&mailbox` | page of `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts,senderEmail`) |
| GET | `/api/v1/chats/{key}?mailbox` | `ChatConversation` (`key,title,initials,members[],threadCount,runs[]`) ; marks read |
| POST | `/api/v1/chats/{key}/messages` | `{body,subject?,asReply?}` (attachments TBD) |
| GET | `/api/v1/me` | user + plan + limits |

Open questions are tracked in `INTERNAL_TODO.md`.
