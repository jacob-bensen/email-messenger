# API contract

**Status (2026-10-19): no mobile REST API exists yet.** This file is the canonical
contract; the section "Implemented JSON/stream endpoints" documents reality, and
"Planned /api/v1" is a proposal (TBD, not implemented in either repo).

## Implemented JSON/stream endpoints (session-cookie auth, CSRF required for POST)
### POST /chats/refresh
Imports new mail for the user's active mailboxes now. 200: `{"imported": <int>}`.
### GET /chats/events
`text/event-stream`; pushes new-mail events (`NewMailEvent`) to the signed-in user. Event payload schema: TBD.
### POST /billing/webhook
Stripe webhook; signature-verified, no session, CSRF-exempt.

## Planned /api/v1 (TBD — to be finalized before any implementation)
Auth: bearer token issued by `POST /api/v1/auth/login` (email+password) — design TBD.
Candidate endpoints mirroring the web workspace, all via the same services as the web controllers:
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` — conversation list (`ConversationListService`)
- `GET /api/v1/chats/{key}` — bubbles (`ChatService`/`ChatConversation`); marks read
- `POST /api/v1/chats/{key}/send` — `{body, subject?, asReply}` (`OutboundMessageService`)
- `GET /api/v1/mailboxes`
Error format: `{ "error": "<code>", "message": "<text>" }` — TBD.
