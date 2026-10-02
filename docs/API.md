# API CONTRACT

**Status (2026-10-02): there is no versioned mobile REST API.** The server exposes
only HTML (Thymeleaf), a few JSON/SSE helpers used by the website's JavaScript, and
a Stripe webhook. The mobile client (`conexus-mail-mobile`) is not yet implemented
and calls nothing. This file records what exists and defines the *planned* contract
skeleton so both repos can build against it. Planned items are marked **PLANNED**
and are not implemented.

## 1. Existing non-HTML endpoints (implemented)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous refresh of the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); new-mail signals (`NewMailEvent`). Event payload shape: TBD (see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe event JSON | `200`; `400` on invalid signature |
| GET | `/actuator/health` | none | — | Spring Boot health |
| GET | `/manifest.webmanifest`, `/sw.js` | none | — | PWA assets |

Errors: `GlobalExceptionHandler` maps exceptions to HTML error pages (e.g. 400 for
`IllegalArgumentException`, 413 for oversized uploads); there is no JSON error
envelope yet.

## 2. Web routes

Page routes (HTML) are inventoried in `WEB.md`; they are not part of the mobile contract.

## 3. PLANNED `/api/v1` (not implemented)

Design constraints (decisions recorded; TBD where noted):

- Auth: bearer token issued by `POST /api/v1/auth/login` (email+password) —
  mechanism (opaque token table vs. JWT) TBD. Google sign-in for mobile TBD.
- JSON error envelope: `{"error": {"code": "<SNAKE_CASE>", "message": "<text>"}}`.
- Endpoints (all call existing services):
  - `GET /api/v1/conversations?mailbox=&q=&filter=&page=` → list items
    (mirrors `ConversationListService`).
  - `GET /api/v1/conversations/{key}` → bubble runs (mirrors `ChatService.buildFor`).
  - `POST /api/v1/conversations/{key}/messages` → send (mirrors `OutboundMessageService`).
  - `POST /api/v1/conversations/{key}/read` → mark read.
  - `GET /api/v1/mailboxes` → connected mailboxes.
  - Live updates: reuse SSE `/chats/events` with bearer auth, or push — TBD.

Nothing above may ship without this file being updated first and both repos
implementing the same shapes.
