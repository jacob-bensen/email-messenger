# API.md — canonical contract

Status (2026-10-12): **no mobile REST API exists.** The server is session-cookie + CSRF
Thymeleaf. This file lists what is actually exposed as non-HTML, then the *planned* `/api/v1`.
Nothing under "Planned" is implemented; client must not call it yet.

## Implemented non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` |
| GET | `/chats/events` | session | — | `text/event-stream` of new-mail events (payload shape TBD — see `NewMailEvent`, `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header; CSRF-exempt | raw Stripe JSON | 200 / error on bad signature (`InvalidStripeSignatureException`) |
| GET | `/actuator/health` | public | — | Spring health JSON |

Errors: `GlobalExceptionHandler` renders an HTML `error` view (e.g. 400 for
`IllegalArgumentException`); there is no JSON error envelope yet.

## Planned /api/v1 (TBD — design before implementing)
- Auth: `POST /api/v1/auth/login` → bearer token (stateless, CSRF-free), refresh, logout.
- `GET /api/v1/chats?mailbox&filter&q&page` → conversation list (reuse `ConversationListService`).
- `GET /api/v1/chats/{key}` → `ChatConversation` as JSON (bubble runs; reuse `ChatService`).
- `POST /api/v1/chats/{key}/send`, `POST /api/v1/chats/{key}/read`.
- `GET /api/v1/mailboxes`; push registration (blocked on Web Push roadmap item).
- Error envelope: `{ "error": { "code": string, "message": string } }`.
