# API contract (canonical)

> **Status: no JSON API is implemented.** The only non-HTML endpoints are
> `POST /chats/refresh` (session+CSRF, returns `{"imported": n}`), `GET /chats/events` (SSE),
> and `POST /billing/webhook` (Stripe, server-to-server). The mobile client has nothing to call yet.
> Everything below marked **PLANNED** is a design proposal, not behavior. Update this file first
> when implementing (contract-first).

## Existing machine endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | Sync active user's mailboxes; `200 {"imported": int}` |
| GET | `/chats/events` | session | `text/event-stream`, `NewMailEvent`s (shape TBD — read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe events |

## PLANNED v1 (`/api/v1`, JSON, bearer token) — TBD
- Auth: `POST /api/v1/auth/login {email,password}` → `{accessToken, expiresAt}` (mechanism TBD).
- `GET /api/v1/chats?q=&mailbox=&filter=&page=` → page of conversation list items (from `ConversationListService`).
- `GET /api/v1/chats/{key}` → conversation (members, bubble runs) (from `ChatService`).
- `POST /api/v1/chats/{key}/send {body, subject?, asReply?}`; `POST /api/v1/chats` (new).
- Errors: `{ "error": "<code>", "message": "..." }` with 400/401/403/404/409/402 (plan limit).

Web routes remain documented in `WEB.md`.
