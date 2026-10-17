# API contract (canonical for backend + conexus-mail-mobile)

**Status: no mobile API is implemented.** Only the endpoints below exist and none is mobile-ready
(session cookie + CSRF). The proposed `/api/v1` surface is a design target, not behavior.

## Existing non-HTML endpoints
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream`; payload TBD (see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | 200 / 400 on bad signature |

## Proposed `/api/v1` (NOT implemented — to be finalized before coding)
Auth: `POST /api/v1/auth/login {email,password}` -> `{accessToken, refreshToken, expiresIn}`;
`Authorization: Bearer`. Errors: `{ "error": "<code>", "message": "<text>" }` with 400/401/403/404/409/429.
- `GET /api/v1/conversations?mailbox&q&filter&page` -> `[ConversationListItem]` (fields as in `service/ConversationListItem`)
- `GET /api/v1/conversations/{key}` -> `ChatConversation` (+ marks read)
- `POST /api/v1/conversations/{key}/messages {body, subject?, asReply?}` -> 201
- `GET /api/v1/mailboxes`
All handlers must delegate to the same services the web controllers use.
