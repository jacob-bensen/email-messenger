# API.md — canonical API contract

Status 2026-10-11: **no JSON API for the mobile client exists.** Both repos depend on
this file; any future endpoint MUST be specified here before implementation.

## Implemented machine-readable endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — triggers an immediate poll of the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`. Event `new-mail` (payload = import count, TBD exact format; see `ChatEventStream`), `: ping` comments as keep-alive |
| POST | `/billing/webhook` | Stripe signature header | Stripe event JSON | 200/400; CSRF-exempt, public |

Errors: HTML error page via `GlobalExceptionHandler` (404 for missing resources,
plan-limit and billing errors render UI). No JSON error envelope exists (TBD).

## HTML endpoints
All other routes return HTML/redirects; listed in `WEB.md`. Not a client contract.

## Planned (NOT implemented — proposal only)
Needed for the Flutter client; shape TBD when first feature is chosen:
- Token auth (`POST /api/v1/auth/login` → bearer token) — requires a stateless
  `SecurityFilterChain` for `/api/**`, CSRF-exempt.
- `GET /api/v1/chats`, `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`
  mirroring `ConversationController`, via a shared service (not the controller).
- JSON error envelope `{ "error": {code, message} }`.
