# API.md — canonical API contract

**Status (2026-11-05): no mobile REST API is implemented.** This file is the
contract both repos depend on; until endpoints are defined here, the mobile
client MUST NOT assume any.

## Existing machine-readable endpoints (not a mobile contract)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session cookie + CSRF | Response `200 {"imported": <int>}` (`Map<String,Integer>`). |
| GET | `/chats/events` | session cookie | `text/event-stream` (SSE); "new mail" signals per user. |
| POST | `/billing/webhook` | Stripe signature | Stripe → server only. |

All other routes return HTML (see `WEB.md`). Errors: `GlobalExceptionHandler` renders `error.html`; no JSON error schema exists (TBD).

## Planned (TBD — define here BEFORE implementing)
Proposed first slice, to be specified in full in the next feature run:
1. Token auth: `POST /api/v1/auth/login` → bearer token (new migration for token storage; CSRF-exempt `/api/**`, stateless chain).
2. `GET /api/v1/chats` (paged list from `ConversationListService`), `GET /api/v1/chats/{key}` (from `ChatService`), `POST /api/v1/chats/{key}/messages` (via `ReplyService`).
3. JSON error envelope `{code, message}`.
