# API CONTRACT (canonical)

Status as of 2026-10-20: **no mobile REST API exists.** The server is a session-cookie + CSRF
Thymeleaf app. This file documents (A) what machine-consumable endpoints exist today and (B) the
planned `/api/v1` surface the mobile repo (`conexus-mail-mobile`) will build against. Section B is
a **proposal, not implemented**; implement server-first, then update the status column.

## A. Existing non-HTML endpoints
| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` | `302 /login` when anonymous; 403 on bad CSRF |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); emitted on `NewMailEvent(userId, importedCount)` — event name/payload shape TBD (see `ChatEventStream`) | `302 /login` |
| POST | `/billing/webhook` | `Stripe-Signature` header, CSRF-exempt | raw Stripe JSON | `200`; `400` missing/invalid signature; `503` webhook not configured | — |

## B. Planned `/api/v1` (not implemented)
Conventions: JSON, `Authorization: Bearer <access token>`, errors `{ "error": "<code>", "message": "..." }`,
server authoritative. All handlers call the existing services (`ChatService`, `ConversationListService`,
`ReplyService`, `MailAccountService`, `PlanLimitService`) — never the web controllers.

| Status | Method | Path | Purpose |
|---|---|---|---|
| planned | POST | `/api/v1/auth/token` | email+password → access/refresh token |
| planned | GET | `/api/v1/chats?cursor=` | conversation list (key, members, last message preview, unread) |
| planned | GET | `/api/v1/chats/{key}` | bubble runs for a conversation |
| planned | POST | `/api/v1/chats/{key}/messages` | send reply (maps to `ReplyService`) |
| planned | GET | `/api/v1/me` | user, plan, limits |

Request/response schemas: TBD until the first endpoint is built (first slice recommended:
`POST /api/v1/auth/token` + `GET /api/v1/chats`).
