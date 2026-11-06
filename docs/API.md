# API.md — canonical API contract

**Status (2026-11-06): there is no REST API for the mobile client.** The backend is session-cookie + CSRF
server-rendered MVC. This file records (1) what machine-readable endpoints exist today and (2) the target
mobile contract as **TBD / proposed**. Nothing under "Proposed" is implemented; the client must not call it yet.

## 1. Existing machine-readable endpoints
Auth for all: session cookie (`JSESSIONID` / remember-me). Unauthenticated → 302 to `/login`. CSRF token required on POST.

### `POST /chats/refresh`
Triggers an immediate import for the signed-in user's active mailboxes.
Response `200 application/json`: `{"imported": <int>}`.

### `GET /chats/events`
Auth: session. `text/event-stream` (SSE). Emits a "new mail" signal when `MailboxPollingService` imports mail
for the user. Payload shape: **TBD** (see `ChatEventStream` / `NewMailEvent`).

### `POST /billing/webhook`
Stripe → server only. Raw JSON + `Stripe-Signature`. CSRF-exempt. Not for clients.

### Errors
`GlobalExceptionHandler` maps `IllegalArgumentException` → 400; oversize upload → 413 (HTML error page, not JSON).
There is no JSON error envelope yet.

## 2. Proposed mobile contract (NOT IMPLEMENTED — TBD)
Prerequisites (see INTERNAL_TODO P0): stateless token auth under `/api/v1/**`, CSRF-exempt, JSON error envelope
`{"error":{"code","message"}}`. Candidate resources, each a thin controller over existing services:

| Method/path | Backed by |
|---|---|
| `POST /api/v1/auth/login` | `UserService` / Spring auth |
| `GET /api/v1/chats?mailbox=&q=&filter=&page=` | `ConversationListService` → `ConversationListItem` |
| `GET /api/v1/chats/{key}` | `ChatService` → `ChatConversation` (+ mark read) |
| `POST /api/v1/chats/{key}/send` | `OutboundMessageService` / `ReplyService` |
| `GET /api/v1/mailboxes` | `MailAccountService` |
| `GET /api/v1/chats/events` (SSE) | `ChatEventStream` |

Schemas to be derived from the records `ConversationListItem` (key, title, initials, memberCount, preview,
threadCount, accounts, senderEmail, …) and `ChatConversation` (key, title, initials, threadCount, bubble runs).
Full field lists: **TBD** — fill when the feature is implemented.
