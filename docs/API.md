# API contract (canonical)

> Status: **no mobile REST API is implemented yet.** The sections below split into
> §Implemented (non-HTML endpoints that exist today) and §Planned (the target contract the
> mobile client will be built against). Update this file *before* implementing anything.

## Implemented

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | 200 `{"imported": <int>}` |
| GET | `/chats/events` | session | — | `text/event-stream`; events published from `NewMailEvent` (payload schema TBD — read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature (`Stripe-Signature` header; CSRF exempt) | raw Stripe JSON | 200 text / 400 `missing Stripe-Signature header` or invalid signature |

All other routes return HTML (see `docs/WEB.md`); errors render `error.html` via `GlobalExceptionHandler`.

## Planned (`/api/v1`, JSON) — TBD, not implemented
Conventions: `Authorization: Bearer <access token>`; errors `{"error": "<code>", "message": "..."}`; ids are numeric; times ISO-8601 UTC.
Needs new token auth (see INTERNAL_TODO #1). First slice proposed:
- `POST /api/v1/auth/login` `{email,password}` → `{accessToken, refreshToken, expiresIn}`
- `GET /api/v1/chats?cursor=` → page of conversation list items (`key`, participants, last bubble preview, unread, timestamp)
- `GET /api/v1/chats/{key}` → bubble runs (`senderName`, `senderEmail`, `outbound`, `bubbles[{id,html,text,sentAt,attachments[]}]`)
- `POST /api/v1/chats/{key}/send` `{body}` → 201 sent bubble
Shapes must be derived from `ChatConversation`/`BubbleRun`/`ConversationListItem` records.
