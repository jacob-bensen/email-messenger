# API.md — canonical API contract

**Status: no JSON API is implemented.** The only machine-facing endpoints today are
`POST /billing/webhook` (Stripe, signature-verified) and `GET /chats/events` (SSE, session-auth).
The mobile client (`conexus-mail-mobile`) cannot talk to the server until a token-authenticated `/api/v1` exists.

This file is the contract both repos build against. Every endpoint added must be specified here *first*.

## Conventions (proposed — TBD until the first endpoint ships)
- Base path `/api/v1`, JSON, UTF-8. Auth: bearer access token (to be designed; session cookies are not suitable).
- Errors: `{"error": {"code": "<UPPER_SNAKE>", "message": "..."}}` with HTTP 400/401/403/404/409/422/429.
- Plan-limit failures reuse `PlanLimitKind` and return 402 `PLAN_LIMIT_EXCEEDED`.

## Planned first slice (not implemented)
1. `POST /api/v1/auth/token` — email+password → access/refresh token.
2. `GET /api/v1/chats?filter=&q=&mailbox=&page=` — backed by `ConversationListService`.
3. `GET /api/v1/chats/{key}` — backed by `ChatService`.
4. `POST /api/v1/chats/{key}/send` — backed by `ReplyService`/`OutboundMessageService`.
