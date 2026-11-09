# API contract (canonical for backend + mobile client)

Status 2026-11-09: **no mobile REST API exists yet.** Section 1 documents what the server really exposes
and is machine-usable today; section 2 is a *planned* contract (nothing in it is implemented — update this
file first, then both repos, when the work is picked up).

## 1. Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE) of new-mail events from `ChatEventStream` (payload shape: TBD, see `NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe JSON | `200`/`400` on bad signature |
| GET | `/actuator/health` | none | - | Spring health JSON |

All other endpoints return HTML/redirects and are listed in `docs/WEB.md`. Auth today = session cookie from
`POST /login` (form fields `email`, `password`, CSRF token required); errors render HTML.

## 2. Planned mobile API (`/api/v1`, TBD — not implemented)
Decisions needed before building (tracked in `INTERNAL_TODO.md`): token scheme (Bearer access + refresh,
stateless filter chain separate from the session chain, CSRF off for `/api/**`), JSON error envelope
`{"error":{"code","message"}}`, pagination style.

Proposed first slice (read-only), all `Authorization: Bearer <token>`, backed by existing services:
- `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}`
- `GET /api/v1/chats?cursor=` → list from `ConversationListService`
- `GET /api/v1/chats/{key}/messages` → `ChatService` / `ChatConversation` (bubble runs)
- `POST /api/v1/chats/{key}/send` → `OutboundMessageService` / `ReplyService`
Exact schemas: TBD.
