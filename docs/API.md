# API.md — canonical API contract (client ⇄ server)

**Status: contract v1 is PLANNED, NOT IMPLEMENTED.** The server exposes no JSON endpoints today
(only the Stripe webhook). Nothing below may be treated as available until the matching
`INTERNAL_TODO.md` item is done and the "Implemented" column is flipped. Both repos MUST follow this file exactly.

## Conventions
- Base path `/api/v1`, JSON (`application/json`), UTC instants as ISO-8601 strings with `Z`.
- Auth (planned): `Authorization: Bearer <accessToken>`; stateless; no CSRF for `/api/**`.
- Errors: `{ "error": { "code": "string", "message": "string" } }` with HTTP 400/401/403/404/409/422/429.
  Codes: `invalid_credentials`, `email_not_verified`, `validation_failed`, `plan_limit_exceeded`, `not_found`, `rate_limited`.
- Server is authoritative; the client never sends computed state (unread, keys, plan limits).

## Endpoints
| Method & path | Auth | Request | Response | Mirrors web | Implemented |
|---|---|---|---|---|---|
| POST `/api/v1/auth/login` | none | `{email,password}` | `{accessToken,refreshToken,expiresIn}` | `/login` | no |
| POST `/api/v1/auth/refresh` | none | `{refreshToken}` | same as login | — | no |
| GET `/api/v1/me` | bearer | — | `{id,email,plan}` | `/account` | no |
| GET `/api/v1/chats?q&mailbox&filter&page` | bearer | — | `{items:[ChatSummary],page,hasMore}` | `GET /chats` | no |
| GET `/api/v1/chats/{key}` | bearer | — | `ChatDetail` | `GET /chats/{key}` | no |
| POST `/api/v1/chats/{key}/messages` | bearer | `{body,subject?,mailboxId?}` | `ChatBubble` | `POST /chats/{key}/send` | no |
| POST `/api/v1/chats` | bearer | `{to:[email],subject?,body,mailboxId?}` | `{key}` | `POST /chats/new` | no |
| GET `/api/v1/mailboxes` | bearer | — | `[Mailbox]` | `/mailboxes` | no |

## DTOs (derived from `ConversationListItem` and the chat view models)
- `ChatSummary`: `{key,title,initials,group,memberCount,preview,lastActivity(Z),unread,threadCount,accounts[],senderEmail}`
- `ChatDetail`: TBD — derive from `ChatConversation`/`BubbleRun`/`BubbleMessage` (fields to be pinned when implemented).
- `ChatBubble`, `Mailbox`: TBD.
- Live updates: existing SSE `GET /chats/events` (session-auth today; bearer support TBD).
