# API contract (canonical)

Status 2026-10-29: **no mobile REST API is implemented.** The mobile client
(`conexus-mail-mobile`) has no endpoints to call. This file documents what exists and the
conventions new `/api/v1/**` endpoints MUST follow. Add each endpoint here BEFORE building it.

## Existing machine-facing endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe events; raw JSON |
| GET | `/chats/events` | session | `text/event-stream` (SSE) "new mail" signal for the web UI |
| GET | `/actuator/health` | none | health |

All other routes are HTML (see `docs/WEB.md`) and are not part of the API contract.

## Conventions for the planned `/api/v1` (TBD, not implemented)
- JSON only; errors as `{ "error": "<CODE>", "message": "..." }` with 400/401/403/404/409/422.
- Auth: TBD — needs a bearer-token scheme (access + refresh) separate from the session/CSRF
  chain; `/api/**` must be excluded from CSRF and return 401 rather than redirecting.
- Server is authoritative; handlers call the same services the Thymeleaf controllers use
  (`ChatService`, `ConversationService`, `ReplyService`, `MailAccountService`, `UserService`).
- Planned first slice: `POST /api/v1/auth/login`, `GET /api/v1/chats`, `GET /api/v1/chats/{key}`,
  `POST /api/v1/chats/{key}/send`. Schemas TBD.
