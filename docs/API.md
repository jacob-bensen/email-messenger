# API.md — canonical API contract

**Status (2026-10-03): no mobile REST API exists.** The server is a
session-cookie, CSRF-protected, server-rendered app. The Flutter client
(`conexus-mail-mobile`) is an empty repo. This file records what is
actually exposed and the TBD design for the first `/api/v1` feature.

## Existing non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream` (SseEmitter via `ChatEventStream.subscribe(userId)`). Signals new mail (`NewMailEvent{userId, importedCount}`). Exact SSE event name/payload: TBD (read `ChatEventStream`). |
| POST | `/billing/webhook` | none; `Stripe-Signature` header | Raw JSON. 200 `ok`; 400 missing/invalid signature; 503 not configured; 500 apply failure (Stripe retries). |
| GET | `/manifest.webmanifest`, `/sw.js`, icons | public | PWA assets |
| GET | `/actuator/health` | public | health |

## Error conventions
HTML error page via `GlobalExceptionHandler` / `error.html`. No JSON error
envelope defined. TBD.

## Planned (not implemented — TBD)
1. Token auth for mobile: `POST /api/v1/auth/login` → access + refresh token
   (needs Spring Security stateless chain under `/api/**`, CSRF-exempt).
2. Read-only `GET /api/v1/chats` and `GET /api/v1/chats/{key}` backed by
   `ConversationListService` / `ChatService`.
3. `POST /api/v1/chats/{key}/send` via `OutboundMessageService`.
Each must be specified here (schemas, errors) before being coded.
