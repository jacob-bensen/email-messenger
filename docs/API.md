# API contract (canonical)

Status: **no mobile REST API is implemented yet.** This file lists (1) what the
server exposes today that a client could use, and (2) the planned `/api/v1`
surface, marked **PLANNED**. Nothing PLANNED may be implemented without
updating this file first; the client in `conexus-mail-mobile` must match it.

## 1. Existing endpoints usable by a client

All require a browser session cookie + CSRF token on POST (no bearer auth).
Responses are HTML except where noted.

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream` (SSE); signals new mail for the user. Event payload schema: TBD (see `NewMailEvent`/`ChatEventStream`). |
| GET | `/actuator/health` | none | JSON health |
| GET | `/manifest.webmanifest` | none | PWA manifest |
| POST | `/billing/webhook` | Stripe signature | Stripe only; not for clients |

HTML-only (not client-consumable): `/chats`, `/chats/{key}`, `/chats/{key}/pane`,
`/chats/new`, `/chats/{key}/send` (multipart: `body`, `subject`, `asReply`,
`attachments[]`, `mailbox`), `/chats/refresh`, `/mailboxes/**`,
`/searches/**`, `/account/**`, `/billing/**`, `/team/**`. Full list: `docs/WEB.md`.

## 2. PLANNED `/api/v1` (not implemented)

Errors: `{ "error": "<code>", "message": "<text>" }` with 400/401/403/404/409/422/429.
Plan-limit violations: 402/403 with `error: "plan_limit_exceeded"`, `kind` = `PlanLimitKind`.

| Method | Path | Purpose | Backed by |
|---|---|---|---|
| POST | `/api/v1/auth/token` | email+password -> bearer token (TBD: Google sign-in) | `UserService` |
| GET | `/api/v1/chats?mailbox=&q=&filter=&page=` | conversation list | `ConversationListService` |
| GET | `/api/v1/chats/{key}` | bubble runs for a conversation | `ChatService` |
| POST | `/api/v1/chats/{key}/messages` | send/reply | `OutboundMessageService` |
| GET | `/api/v1/mailboxes` | connected mailboxes | `MailAccountService` |
| GET | `/api/v1/events` | SSE, bearer-authenticated | `ChatEventStream` |

Request/response JSON schemas: **TBD** — define here before implementing.
