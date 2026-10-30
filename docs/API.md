# API contract (canonical)

Status as of 2026-10-30: **the backend exposes no REST API for the mobile client.** Section 1 lists the
non-HTML endpoints that exist today. Section 2 is a PROPOSED contract; nothing in it is implemented on either
side. Implement server-first, then update the client, and flip each item's status here in the same run.

## 1. Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` |
| GET | `/chats/events` | session | none | `text/event-stream` of new-mail signals (payload shape: TBD, see `NewMailEvent`/`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header | Stripe event JSON | `200` / `400` invalid signature |
| GET | `/actuator/health` | none | | Spring health JSON |

Errors on the web paths are HTML (`error.html`) via `GlobalExceptionHandler`; no JSON error envelope exists (TBD).

## 2. PROPOSED `/api/v1` (not implemented)
Auth: `POST /api/v1/auth/token` `{email,password}` → `{accessToken, refreshToken, expiresIn}`; Bearer auth on all
other routes; stateless, CSRF-exempt under `/api/**`. Error envelope: `{"error":{"code":"...","message":"..."}}`
with 400/401/403/404/409/429.

| Status | Endpoint | Backed by service |
|---|---|---|
| PROPOSED | `GET /api/v1/chats?q&mailbox&filter&page` → list items + counts | ConversationListService |
| PROPOSED | `GET /api/v1/chats/{key}` → `ChatConversation` (members, bubble runs) and marks read | ChatService |
| PROPOSED | `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` | OutboundMessageService |
| PROPOSED | `GET /api/v1/mailboxes` | MailAccountService |
| PROPOSED | `GET /api/v1/me` → user, plan, limits | UserService, PlanLimitService |
DTO field-level schemas: TBD — derive from `ConversationListItem`, `ChatConversation`, `BubbleRun`, `BubbleMessage`, `ChatMember`.
