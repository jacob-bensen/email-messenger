# API.md — canonical contract (backend ↔ mobile)

Status: **there is no mobile REST API yet.** §1 documents what exists and is callable
with a browser session; §2 is the *planned* `/api/v1` surface, marked PLANNED and not
implemented on either side. Do not code against §2 until it is moved to §1.

## 1. Implemented (session-cookie auth, CSRF required on POST)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's due mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; emits new-mail signals (`NewMailEvent`) plus keepalive every 25s. Event payload shape: TBD (read `ChatEventStream`) |

All other routes return HTML (see WEB.md). Errors: unauthenticated → 302 `/login`;
CSRF failure → 403.

## 2. PLANNED `/api/v1` (not implemented)

Auth: bearer access token + refresh token (new; Spring Security stateless chain for
`/api/**`, CSRF off there). All endpoints scoped to the authenticated user.

- `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}`
- `GET  /api/v1/chats?mailbox=&q=&filter=&page=` → page of `ConversationListItem`
- `GET  /api/v1/chats/{key}` → `ChatConversation` (key, title, members, runs/bubbles)
- `POST /api/v1/chats/{key}/read`
- `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` → created bubble
- `GET  /api/v1/mailboxes`
- Errors: `{ "error": "<code>", "message": "..." }`; 401 unauth, 403 plan limit
  (`PlanLimitExceededException`), 404, 422 validation.

Each endpoint must be backed by the existing service (`ChatService`,
`ConversationListService`, `OutboundMessageService`) — never by controller logic.
