# API.md — canonical API contract

Status 2026-11-10: **no mobile REST API exists yet.** This file documents (1) what the server exposes today that
a client could call, and (2) the planned `/api/v1` surface, marked TBD. Update this file BEFORE implementing.

## 1. Existing non-HTML endpoints
| Method/Path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| `GET /chats/events` | session | – | `text/event-stream` (SSE) fed by `NewMailEvent(userId, importedCount)`; exact event name/payload: TBD (see `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header | Stripe event JSON | 2xx; no CSRF |
| `GET /actuator/health` | public | – | Spring health JSON |

All other routes return HTML (see `WEB.md`) and use form posts + CSRF + session cookies.

## 2. Planned `/api/v1` (TBD — not implemented)
Conventions to adopt: JSON, `Authorization: Bearer <token>` (token issuance TBD: new `api_token` table vs. JWT),
errors as `{"error": "<code>", "message": "..."}` with 400/401/403/404/409/429.
Candidate endpoints, all delegating to existing services:
- `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`
- `GET /api/v1/mailboxes`
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` → list of `ConversationListItem`
- `GET /api/v1/chats/{key}` → `ChatConversation` (bubble runs, participants)
- `POST /api/v1/chats/{key}/send` → body, subject?, asReply?
- `POST /api/v1/chats/refresh`; `GET /api/v1/chats/events` (SSE)
Schemas: TBD — derive from `service/ChatConversation`, `BubbleRun`, `ConversationListItem` when implementing.
