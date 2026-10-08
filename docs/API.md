# API.md — canonical API contract

**Status (2026-10-08): no mobile REST API exists yet.** This file documents the HTTP surface that actually exists and
marks the mobile contract as TBD. Nothing below `## Planned` is implemented; the client must not depend on it until it
is moved out of Planned in the same commit that implements it on both sides.

## Conventions today
- Auth: session cookie via `POST /login` (form: `email`, `password`, optional `remember-me`) -> 302. Unauthenticated -> 302 `/login`.
- CSRF token required on all non-GET requests except `/billing/webhook`. Forms carry `_csrf`.
- Errors: HTML error page (`error.html`, `GlobalExceptionHandler`); no JSON error envelope.

## Existing JSON/stream endpoints
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | `200 {"imported": <int>}` (triggers poll of caller's mailboxes) |
| GET | `/chats/events` | session | `text/event-stream`, new-mail events (`NewMailEvent`); payload shape TBD |
| POST | `/billing/webhook` | Stripe signature | Stripe event ingest (server-to-server) |
| GET | `/actuator/health` | none | Spring health |

All other routes return HTML; see `WEB.md` for the route inventory.

## Planned (NOT implemented) — `/api/v1`
Design intent, to be finalised when the first mobile feature is built:
1. Bearer-token auth (`POST /api/v1/auth/login` -> access/refresh tokens), CSRF-exempt, stateless filter chain scoped to `/api/**`.
2. `GET /api/v1/conversations?mailbox=&q=&filter=&page=` backed by `ConversationListService.list`.
3. `GET /api/v1/conversations/{key}` backed by `ChatService.buildFor`.
4. `POST /api/v1/conversations/{key}/messages` and `POST /api/v1/conversations` backed by `OutboundMessageService`/`ReplyService`.
5. Error envelope `{ "error": { "code": string, "message": string } }` with 401/403/404/409(plan limit)/422.
Request/response schemas: TBD.
