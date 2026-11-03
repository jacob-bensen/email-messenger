# API contract

**Status: no mobile REST API is implemented yet.** This file is canonical; both repos must follow it. Entries marked *(exists)* are implemented today; everything under "Planned v1" is TBD and must be agreed here before any code is written.

## Existing non-HTML endpoints *(exists, session-cookie + CSRF)*
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream` of new-mail events (payload shape: TBD, see `NewMailEvent`/`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe webhook; not for clients |

## Planned v1 (TBD — not implemented)
Conventions proposed: base `/api/v1`, JSON, `Authorization: Bearer <token>`, errors as `{"error":{"code":"...","message":"..."}}`.

1. `POST /api/v1/auth/login` — `{email,password}` → `{accessToken,refreshToken,expiresIn}` (needs new token auth; TBD)
2. `GET /api/v1/chats` — conversation list (backed by `ConversationListService`)
3. `GET /api/v1/chats/{key}` — `ChatConversation` (key, title, initials, members, runs)
4. `POST /api/v1/chats/{key}/send` — reply (backed by `OutboundMessageService`/`ReplyService`)

Shape details are deliberately TBD until the first mobile feature run chooses a slice.
