# API.md — canonical API contract

**Status: no mobile REST API exists yet.** This file documents (1) the JSON/stream endpoints that
exist today and (2) the planned `/api/v1` surface (marked PLANNED — not implemented; do not code
clients against it until the section is marked IMPLEMENTED).

Auth today: session cookie + CSRF token (browser only). Mobile auth: TBD (PLANNED: bearer tokens).

## Implemented
### POST /chats/refresh
Auth: session + CSRF. Imports new mail for the caller's active mailboxes now.
Response 200: `{"imported": <int>}`.

### GET /chats/events
Auth: session. `text/event-stream` of live new-mail signals (`NewMailEvent`) for the caller.
Event payload shape: TBD (see `ChatEventStream`).

All other routes return HTML — see `WEB.md`.

## PLANNED (v1, not implemented)
| Method | Path | Purpose | Backed by |
|---|---|---|---|
| POST | /api/v1/auth/token | email+password → bearer token | `UserService`/auth |
| GET | /api/v1/chats?mailbox=&q=&filter=&page= | conversation list | `ConversationListService` |
| GET | /api/v1/chats/{key} | messages as bubble runs | `ChatService` |
| POST | /api/v1/chats/{key}/send | send message `{body, subject?, asReply}` | `OutboundMessageService` |
| GET | /api/v1/mailboxes | connected mailboxes | `MailAccountService` |
Schemas and error codes: TBD — define here before implementing.
