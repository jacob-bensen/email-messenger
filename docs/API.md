# API contract (canonical)

**Status: no mobile REST API is implemented yet.** The server is a session-authenticated Thymeleaf app with CSRF.
Nothing below under "Planned" exists in code; it is a TBD placeholder to be defined contract-first in a future run.

## Existing machine-readable endpoints
| Method/Path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — newly imported message count for the active user |
| `GET /chats/events` | session | none | `text/event-stream` (SSE); new-mail events from `ChatEventStream` (event payload shape: TBD, read `NewMailEvent`/`ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header | Stripe event JSON | `200`; `400` on invalid signature. CSRF-exempt |
| `GET /actuator/health` | public | — | Spring Actuator health |

Errors: handled by `GlobalExceptionHandler` / `/error` as HTML; no JSON error envelope exists (TBD).

## Planned `/api/v1` (TBD — not implemented)
Needs: token auth (bearer/JWT or API session), JSON error envelope, and endpoints for conversations list, conversation messages, send, mailbox list, plan/limits, push registration. Define here before coding.
