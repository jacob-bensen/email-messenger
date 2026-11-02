# API contract (canonical)

**Status: no mobile REST API exists yet.** The server currently exposes only
session-authenticated, CSRF-protected HTML endpoints (see `WEB.md`) plus:

- `GET /chats/events` — SSE stream of new-mail signals (session cookie auth).
- `POST /billing/webhook` — Stripe webhook (signature-verified, no auth/CSRF).
- `GET /actuator/health` — public health check.

Nothing else is a stable machine contract. All JSON schemas, bearer/token auth,
and error-code conventions for the mobile client are **TBD**.

## Proposed v1 (not implemented — design target)
Prefix `/api/v1`, JSON, bearer-token auth. Each endpoint must delegate to the
existing services, never fork logic from the web controllers.

| Method | Path | Backed by | Notes |
|---|---|---|---|
| POST | /api/v1/auth/login | UserService / Spring auth | returns access+refresh token (TBD) |
| GET | /api/v1/chats | ConversationListService | paging `page`, `q`, `mailbox`, `filter` |
| GET | /api/v1/chats/{key} | ChatService | bubble runs for a conversationKey |
| POST | /api/v1/chats/{key}/messages | OutboundMessageService | body, subject?, asReply? |
| GET | /api/v1/mailboxes | MailAccountService | |
| GET | /api/v1/chats/events | ChatEventStream | SSE, token auth |

Errors: `{ "error": "<code>", "message": "<text>" }` with standard HTTP status
(TBD until implemented).
