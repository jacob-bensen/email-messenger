# API contract (canonical)

**Status: no mobile/REST API is implemented.** The server exposes only the
session-authenticated Thymeleaf website and three non-HTML endpoints:

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | `text/event-stream` (new-mail events; payload shape TBD) |
| POST | `/billing/webhook` | Stripe signature | Stripe event JSON | 2xx / 400 on bad signature |

These are web-internal, not a stable client contract.

## Planned `/api/v1` (TBD — no behavior implemented yet)
Before any client work, the following must be specified here first:
1. Token auth for mobile (login → access/refresh tokens; Google sign-in).
2. Resources: mailboxes, conversations (by `conversationKey`), messages,
   send/reply, plan/limits, live updates (SSE or push).
3. Error envelope and status codes.
4. Mapping to existing services (`ChatService`, `ReplyService`,
   `OutboundMessageService`, `MailAccountService`, `PlanLimitService`) — the
   REST controllers must reuse them, not fork logic.
