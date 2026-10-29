# API contract (canonical for backend + mobile client)

Status (2026-10-29): **no versioned REST API exists.** Nothing here may be assumed by the
client beyond the "Existing machine-readable endpoints" section. The "Planned v1" section
is a design proposal, not implemented; implement it contract-first (update this file in
the same commit as code).

## Existing machine-readable endpoints (session-cookie auth, CSRF-protected for POST)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — newly imported messages after a synchronous mailbox refresh |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); signals new mail for the user (`ChatEventStream`). Event `new-mail`, data `{"imported":<int>}`; periodic `ping` comment keepalive |
| GET | `/actuator/health` | public | none | Spring Boot health JSON |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 200/400 (server-to-server only) |

Errors: unauthenticated requests get a `302` to `/login` (HTML), not a JSON 401.
`GlobalExceptionHandler` renders HTML error pages. Both are unsuitable for a mobile client.

## Planned v1 (NOT IMPLEMENTED) — `/api/v1`

Auth: `POST /api/v1/auth/token` (email+password → bearer token), stateless, no CSRF.
Errors: `{"error": {"code": "<SNAKE_CASE>", "message": "<text>"}}` with 401/403/404/422/429.

- `GET /api/v1/chats?mailbox=&q=&filter=&page=` → page of conversation list items
  (maps to `ConversationListService.list`; filter ∈ `all|unread|…` per `ChatFilter`).
- `GET /api/v1/chats/{key}` → conversation with bubble runs (maps to `ChatService.buildFor`); marks read.
- `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` → send (maps to the
  `ConversationController.send` logic, to be extracted into a service first).
- `GET /api/v1/mailboxes` → connected accounts.

DTO field-level schemas: TBD until the service extraction defines the records.
