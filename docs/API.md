# API — canonical contract (server ↔ mobile client)

> Status: **no mobile REST API is implemented.** This file is the contract both
> repos must follow; endpoints below marked *planned* do not exist yet. Do not
> implement client code against *planned* endpoints until they are built and
> the status here flips to *implemented*.

## Existing JSON/stream endpoints (session-cookie auth, CSRF token required on POST)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session | `200 {"imported": <int>}` — synchronous mailbox refresh |
| GET | `/chats/events` | session | `text/event-stream`; `NewMailEvent` messages |
| POST | `/billing/webhook` | Stripe signature | Stripe → server only |

## Planned `/api/v1` (design, TBD details)
- Auth: bearer token (access + refresh) issued by `POST /api/v1/auth/login`; TBD whether Google sign-in is native or via web flow.
- `GET /api/v1/chats?mailbox=&filter=&q=&page=` — conversation list (backed by `ConversationListService`).
- `GET /api/v1/chats/{key}` — bubble runs for one conversation (backed by `ChatService`/`IMTransformService`).
- `POST /api/v1/chats/{key}/read`, `POST /api/v1/chats/{key}/send` (`ReplyService`).
- `GET /api/v1/mailboxes`.
- Errors: JSON `{ "error": "<code>", "message": "<text>" }`; 401 unauthenticated, 403 plan limit (`PlanLimitExceededException`), 404 unknown key, 422 validation.

Exact DTO schemas: **TBD** — define here before implementing either side.
