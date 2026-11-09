# API.md — canonical API contract

**Status (2026-11-09): there is NO mobile/REST API yet.** The server is session-cookie + CSRF, HTML-first.
The mobile repo (`conexus-mail-mobile`) is an empty scaffold. The `/api/v1` contract below is a **PROPOSAL (TBD)**
and is not implemented on either side; do not code against it until it is moved out of "Proposed".

## A. Implemented non-HTML endpoints (current reality)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/chats/events` | session | — | `text/event-stream` (SSE) of new-mail signals (`ChatEventStream`, per user). Payload shape TBD (read `ChatEventStream`). |
| POST | `/chats/refresh` | session + CSRF | none | JSON `Map<String,Integer>` (counts from a manual poll; keys TBD — see `ConversationController.refresh`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event JSON | 2xx on success; invalid signature → rejected (`InvalidStripeSignatureException`) |
| GET | `/actuator/health` | public | — | Spring Actuator health |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, icons | public | — | PWA assets |

Errors: `GlobalExceptionHandler` renders HTML error pages (404 for not-found/unauthorized resources). No JSON error envelope exists.

## B. Proposed `/api/v1` (NOT IMPLEMENTED — TBD)
Needs a design decision first (token auth vs. session; recommendation: short-lived access token + refresh token,
stored hashed, new Flyway migration; separate stateless `SecurityFilterChain` for `/api/**`, CSRF off).
Candidate first slice (read-only, reuses existing services):
- `POST /api/v1/auth/login` → `{accessToken, refreshToken, expiresIn}`
- `GET /api/v1/conversations?page=&q=&mailbox=` → page of `ConversationListItem`
  (`key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail`)
- `GET /api/v1/conversations/{key}` → `ChatConversation` (bubble runs)
- `POST /api/v1/conversations/{key}/messages` → send reply (`ReplyService`)
Error envelope proposal: `{ "error": "<code>", "message": "<text>" }` with 400/401/403/404/409/429.

## Sync audit
Client calls: none (no client code). Server/contract mismatches: see `INTERNAL_TODO.md`.
