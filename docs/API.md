# API contract (canonical)

Status: **v0 — documents what is implemented today.** There is no dedicated mobile REST API yet.
Anything under "Planned" is a proposal and must be agreed here BEFORE implementation in either repo.
Unknown details are marked TBD.

## Conventions
- Auth: session cookie (see BACKEND.md). Unauthenticated requests redirect to `/login` (302), not 401.
- CSRF: required on all non-GET calls except `/billing/webhook`.
- Errors: no JSON error envelope; `GlobalExceptionHandler` renders `error.html` (TBD codes per route).

## Implemented JSON / non-HTML endpoints
### `POST /chats/refresh`
Auth: session + CSRF. Triggers a synchronous import for the user's active mailboxes.
Response `200 application/json`: `{"imported": <int>}` (newly imported messages).

### `GET /chats/events`
Auth: session. `text/event-stream`. Event `new-mail`, data `{"imported": <int>}`.
Comment line `: ping` every 25 s (`mailbox.sse.keepalive-ms`). Client reconnects on drop.

### `POST /billing/webhook`
Stripe-only (signature header). Not for clients.

## Implemented HTML endpoints relevant to a client (form-encoded)
| Method | Path | Notes |
|---|---|---|
| POST | `/login` | form: `email`, `password`, `remember-me` |
| POST | `/register` | `RegistrationForm` (TBD fields) |
| GET | `/chats?mailbox=&q=&filter=&page=` | conversation list (HTML) |
| GET | `/chats/{key}` , `/chats/{key}/pane` | conversation view / fragment; marks read |
| POST | `/chats/{key}/send` | multipart: `body`(≤100000), `subject?`, `asReply?`, `attachments[]`, `mailbox?`; body or attachment required; ≤10MB/file, 25MB/request |
| POST | `/chats/new` | new conversation (TBD fields) |
| GET/POST | `/mailboxes`, `/mailboxes/{id}/sync`, `/mailboxes/{id}/delete`, `/mailboxes/gmail/*` | mailbox management |
| POST | `/searches`, `/searches/{id}/delete` | saved searches |

## Planned (not implemented) — mobile API v1 proposal
`/api/v1/auth/token` (email+password → bearer), `/api/v1/conversations`, `/api/v1/conversations/{key}/messages`,
`POST …/messages`, SSE `/api/v1/events`. Requires: bearer-token security chain without CSRF, JSON DTOs,
and extraction of controller orchestration into services (see INTERNAL_TODO #1, #2).
