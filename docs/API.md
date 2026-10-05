# API contract (canonical)

**Status: no mobile/REST API is implemented.** The backend only serves
server-rendered HTML (see `WEB.md`). This file records (1) the JSON/streaming
endpoints that exist today and (2) the contract-first stub the mobile client
must wait on. Last audited 2026-10-05.

## 1. Endpoints that exist and return non-HTML

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — polls the user's active mailboxes now |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); emits new-mail events published by `MailboxPollingService` / `ImapIdleService`. Event payload schema: TBD (see `NewMailEvent`, `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | `200` on success; invalid signature rejected (`InvalidStripeSignatureException`) |

Errors: handled by `GlobalExceptionHandler` and render `error.html` (HTML, not
JSON). A JSON error envelope does not exist yet.

## 2. Planned mobile API (NOT IMPLEMENTED — design TBD)

Prerequisites before any endpoint is defined here:
1. Token auth (e.g. `/api/v1/auth/token` + bearer filter) alongside the cookie session.
2. JSON error envelope `{ "error": { "code", "message" } }`.
3. `/api/v1/**` controllers that delegate to the existing services
   (`ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`,
   `MailAccountService`) — never duplicating controller logic.

Candidate first slice: `GET /api/v1/chats`, `GET /api/v1/chats/{key}`,
`POST /api/v1/chats/{key}/send`. Schemas to be written here first.
