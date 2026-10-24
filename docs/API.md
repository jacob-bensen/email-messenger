# API contract (canonical)

**Status 2026-10-24: no mobile REST API exists.** The server is a server-rendered
Thymeleaf app with cookie-session + CSRF auth. The mobile repo
(`conexus-mail-mobile`) is an empty scaffold. This file documents the
machine-readable endpoints that actually exist, then the `/api/v1` surface that
is **proposed (TBD, not implemented)**. Both repos must change together when any
of it is built; update this file first.

## Existing machine-readable endpoints (implemented)

### `POST /chats/refresh`
Auth: session cookie + CSRF token. Request: empty body. Triggers a synchronous
poll of the caller's active mailboxes.
Response `200 application/json`: `{"imported": <int>}`.
Errors: `302 /login` if unauthenticated, `403` on missing CSRF.

### `GET /chats/events`
Auth: session cookie. `text/event-stream`. Event `new-mail`, data
`{"imported": <int>}` — published when polling/IDLE imports new messages for the
user.

### `POST /billing/webhook`
Stripe-only, signature-verified, CSRF-exempt. Not for clients.

## Proposed `/api/v1` (TBD — not implemented)
Minimum slice to make a mobile client useful. Shapes are provisional until the
first feature run implements them.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/token` | email+password → `{accessToken, refreshToken, expiresIn}` |
| POST | `/api/v1/auth/refresh` | rotate tokens |
| GET | `/api/v1/me` | user, plan, limits |
| GET | `/api/v1/conversations?cursor=` | list items (key, participants, last-message preview, unread, updatedAt) |
| GET | `/api/v1/conversations/{key}/messages?cursor=` | bubble runs / messages |
| POST | `/api/v1/conversations/{key}/messages` | send (body, optional subject/asReply) |
| GET | `/api/v1/events` | SSE `new-mail` (bearer auth) |

Error envelope (proposed): `{"error": "<code>", "message": "<text>"}` with
standard HTTP statuses. Services to reuse (no logic forking):
`ConversationListService`/`ChatService`, `ReplyService`, `UserService`,
`PlanLimitService`.
