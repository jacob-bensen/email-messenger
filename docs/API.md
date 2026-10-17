# API contract

**Status: no mobile JSON API exists yet.** This file is the canonical contract; today it records what is actually exposed and the *planned* v1 surface (marked PLANNED, not implemented). The mobile repo must not code against PLANNED items until they are ticked here.

## Existing machine-facing endpoints (implemented)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | Sync now. Response `200 {"imported": <int>}` |
| GET | `/chats/events` | session | SSE stream; `NewMailEvent` per user |
| POST | `/billing/webhook` | Stripe signature | CSRF-exempt; Stripe only |
| GET | `/actuator/health` | none | health |
| GET | `/manifest.webmanifest`, `/sw.js` | none | PWA |

Every other route is an HTML page (see `WEB.md`) using form posts and redirects — **not** a stable API.

## PLANNED v1 (`/api/v1`, not implemented)
Bearer-token auth (login → access/refresh token), JSON DTOs, stateless (no CSRF).
- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` → conversation list (backed by `ConversationListService`)
- `GET /api/v1/chats/{key}` → bubble runs (backed by `ChatService`)
- `POST /api/v1/chats/{key}/send` (backed by `OutboundMessageService`)
- `GET /api/v1/mailboxes`, `GET /api/v1/me` (plan + limits)
- Error body: `{"error": "<code>", "message": "..."}`; 401 unauthenticated, 403 plan limit, 404 unknown key, 422 validation.
DTO schemas TBD — define here first when the feature run starts.
