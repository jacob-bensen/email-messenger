# API.md — canonical contract (client ⇄ server)

**Status 2026-10-10: no mobile REST API is implemented.** This file records (1) the machine
endpoints that exist today and (2) the planned `/api/v1` surface, clearly marked PLANNED.
Never mark an endpoint implemented until the controller and a test exist.

## Implemented (session-cookie auth, CSRF required on POST)
| Method/Path | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — messages imported synchronously from the user's active mailboxes | 302→/login if unauthenticated; 403 bad CSRF |
| `GET /chats/events` | session | none | `text/event-stream` (SSE); emits a signal when `MailboxPollingService` imports new mail for the user (payload shape TBD — read `ChatEventStream`/`NewMailEvent`) | 302→/login |
| `POST /billing/webhook` | Stripe signature header | raw Stripe JSON | 200 | 400 invalid signature |

All other routes return HTML (see WEB.md); they are not part of the client contract.

## PLANNED `/api/v1` (not implemented — design only)
Auth: needs a non-cookie scheme (bearer access token + refresh token, `POST /api/v1/auth/login`
→ `{accessToken, refreshToken, expiresIn}`); CSRF-exempt under `/api/**` with stateless filter chain.
Errors: `{ "error": "<code>", "message": "..." }` with 401/403/404/409/422/429 (plan limit = 402 or 403 TBD).
Initial resource set, mirroring existing records: `GET /api/v1/chats` (`ConversationListItem`),
`GET /api/v1/chats/{key}` (`ChatConversation`), `POST /api/v1/chats/{key}/messages`,
`GET /api/v1/mailboxes`. Field lists are TBD until the first slice is built; the JSON shape of
`ConversationListItem`/`ChatConversation` (service package) is the starting point.
