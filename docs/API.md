# API contract

Canonical contract between the backend, website and the mobile client
(`conexus-mail-mobile`). Status 2026-10-29: **no mobile REST API is
implemented.** The first section documents what exists; the second is a
proposal (TBD — not implemented, not to be coded against until promoted).

## 1. Existing HTTP surface
- Machine-facing: `POST /billing/webhook` (Stripe, signature header, CSRF-exempt,
  public) and `GET /chats/events` (SSE `text/event-stream`, session auth).
  Event payload shape: TBD (see `ChatEventStream` / `NewMailEvent`).
- Everything else returns HTML and uses session cookie + CSRF token
  (`docs/WEB.md` lists routes). Not a stable client contract.
- Errors: `GlobalExceptionHandler` renders `error.html`; no JSON error schema.

## 2. Proposed mobile API (TBD)
Prefix `/api/v1`, JSON, bearer-token auth (new). Minimum first slice:
- `POST /api/v1/auth/login` {email,password} → {token, expiresAt}
- `GET  /api/v1/chats` → list of conversation summaries (key, title, last
  message preview, unread, updatedAt) via `ConversationListService`
- `GET  /api/v1/chats/{key}` → bubble runs via `ChatService`
- `POST /api/v1/chats/{key}/send` → via `ReplyService`/`OutboundMessageService`
Error body: {code, message}. These must call the same services the web
controllers use.
