# API contract

**Status (2026-10-19): there is no JSON REST API for a mobile client.** The
backend exposes a server-rendered website plus a few machine endpoints. This
file documents what actually exists (§1), then the **proposed** mobile
contract (§2), which is **NOT IMPLEMENTED** on either side. When §2 is
implemented, move each endpoint into §1 in the same commit on both repos.

Conventions today: session-cookie auth, CSRF token on every state-changing
request unless noted, form-encoded bodies, HTML/redirect responses.

## 1. Implemented machine-consumable endpoints

### POST /chats/refresh
- Auth: session. CSRF required.
- Effect: synchronously polls the user's connected mailboxes.
- 200 `application/json`: `{"imported": <int>}` — count of newly imported messages.

### GET /chats/events
- Auth: session. `text/event-stream`, server timeout 30 min (client reconnects).
- Event `new-mail`, data `{"imported": <int>}`, emitted after commit of an import
  for that user. Also comment-only keep-alive pings.

### POST /billing/webhook
- Auth: none; `Stripe-Signature` header. CSRF exempt. Body: raw Stripe event JSON.
- 200 `ok`; 400 missing/invalid signature; 503 webhook not configured; 500 on
  processing failure (Stripe retries).

### Form/HTML endpoints
All routes in `WEB.md` are HTML. Notable parameters:
- `POST /login`: `email`, `password`, `remember-me`. Failure → `/login?error`.
- `POST /register`: see `RegistrationForm`.
- `GET /chats?page&q&mailbox&filter` (page size 30).
- `POST /chats/new`: `to`, `subject`, `body`, `attachments[]` (multipart), `mailbox`.
- `POST /chats/{key}/send`: `body`, `subject`, `asReply`, `attachments[]`, `mailbox`.
- Error codes: validation failures re-render the page with 200; unknown chat → redirect `/chats`.

## 2. PROPOSED mobile API (`/api/v1`) — NOT IMPLEMENTED

Shapes derive from existing service records so web and API share one service
layer. All TBD items need a decision before implementation.

- Auth: **TBD** — token auth (e.g. `POST /api/v1/auth/login` → access+refresh
  token), CSRF-exempt `/api/**` chain, same `LoginThrottleService`.
- `GET /api/v1/conversations?q&mailbox&filter&page` → page of
  `ConversationListItem {key,title,initials,group,memberCount,preview,lastActivity(epochMs),unread,threadCount,accounts[],senderEmail}`.
- `GET /api/v1/conversations/{key}` → `ChatConversation {key,title,initials,members[ChatMember{email,displayName,initials,label,signatureHtml}],threadCount,runs[...]}` (marks read).
- `POST /api/v1/conversations/{key}/messages` → send (body, subject, asReply).
- `GET /api/v1/mailboxes`, plan/limits endpoint — **TBD**.
- Live updates: reuse SSE `/chats/events`, or push — **TBD**.
- Errors: **TBD** (`{error:{code,message}}` suggested).
