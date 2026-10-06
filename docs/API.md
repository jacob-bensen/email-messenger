# API.md — canonical API contract

Status: **no mobile REST API exists yet.** This file records (A) JSON/stream endpoints that
exist today, and (B) the planned `/api/v1` surface. Both repos must be updated together
with any change here. Items in (B) are PLANNED, not implemented.

## A. Implemented (session-cookie auth, CSRF token required on POST)

### POST /chats/refresh
Auth: session. Triggers a synchronous poll of the user's active mailboxes.
200 `{"imported": <int>}`. Unauthenticated → 302 to `/login`.

### GET /chats/events  (SSE, `text/event-stream`)
Auth: session. Event `new-mail`, data `{"imported": <int>}` when new mail is imported.

### POST /billing/webhook
Stripe-only (signature header, CSRF exempt, public). Not for the client.

All other routes return HTML (see `WEB.md`). Errors are rendered as `error.html`; no JSON
error schema exists (TBD).

## B. Planned `/api/v1` (design, not implemented)
Mirrors view models in `service/`: `ConversationListItem`, `ChatConversation`,
`ChatMember`, `BubbleRun`, `BubbleMessage`.
- Auth: TBD — needs bearer/token auth (login → access + refresh token); cookie+CSRF is unsuitable.
- `GET /api/v1/chats?q&mailbox&filter&page` → page of ConversationListItem
- `GET /api/v1/chats/{key}` → ChatConversation (marks read)
- `POST /api/v1/chats/{key}/messages` → send (body, subject?, asReply?)
- Error shape: `{"error": "<code>", "message": "<text>"}` (proposed)
