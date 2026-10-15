# API contract

**Status (2026-10-15): there is NO JSON REST API.** The server exposes only
server-rendered HTML, form POSTs (CSRF + session cookie) and one SSE stream.
The mobile client cannot authenticate (no token auth) or consume data in a
typed way. This file is the canonical place to define the contract; nothing
below `/api/v1` exists yet.

## Existing non-HTML interfaces
| Method & path | Auth | Notes |
|---|---|---|
| `GET /chats/events` | session | `text/event-stream`; `NewMailEvent` per new mail for the user's accounts. Payload schema: TBD (read `ChatEventStream`). |
| `POST /billing/webhook` | Stripe signature | Stripe → server only. |
| `GET /manifest.webmanifest`, `/sw.js` | public | PWA. |

All other endpoints are HTML — see `docs/WEB.md`.

## Planned `/api/v1` (TBD — design in a future run, contract-first)
Proposed first slice, to be specified here before implementation:
1. `POST /api/v1/auth/token` — email+password → bearer token (needs new token table + filter).
2. `GET /api/v1/chats?page&q&mailbox&filter` — conversation list (mirror `ConversationListItem`).
3. `GET /api/v1/chats/{key}` — bubble runs (mirror `ChatConversation`/`BubbleRun`).
4. `POST /api/v1/chats/{key}/send` — reply (delegate to `ReplyService`).
Error envelope (proposed): `{ "error": "<code>", "message": "<text>" }` with 401/403/404/409/422/429.
