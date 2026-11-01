# API.md — canonical API contract

Status 2026-11-01: **there is no REST API for the mobile client yet.** The server is a
server-rendered web app (see WEB.md). This file records (A) the machine-readable
endpoints that exist today and (B) the planned mobile contract, which is TBD and must be
fully specified here *before* any implementation (routine step "contract first").

## A. Existing non-HTML endpoints (session-cookie auth + CSRF)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session, CSRF token | `200 {"imported": <int>}` — triggers an immediate poll of the user's active mailboxes |
| GET | `/chats/events` | session | `text/event-stream` (SseEmitter); new-mail signals for the user. Event names/payload: TBD (see `ChatEventStream`, `NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header | Stripe webhook receiver; not for clients |

Errors: handled by `GlobalExceptionHandler` / `error.html` (HTML). No JSON error envelope exists.

## B. Planned mobile API (TBD — not implemented)
Decisions needed before building: token auth scheme (e.g. bearer access + refresh token,
new Flyway table), base path `/api/v1`, JSON error envelope, DTOs for: login, conversations
list, conversation (bubble runs), send reply, mailbox list, SSE/push for live updates.
Proposed first slice for the next run: `POST /api/v1/auth/login`, `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send` — backed by `ChatService` /
`OutboundMessageService`.
