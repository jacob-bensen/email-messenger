# API contract (canonical for the mobile client)

**Status: no JSON API exists.** The backend exposes only session-authenticated
HTML (Thymeleaf) endpoints, one SSE stream and a Stripe webhook. Nothing below
"Planned" is implemented; do not code against it until it is moved to
"Implemented" in the same commit as the server code.

## Implemented non-HTML surfaces
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream`; `NewMailEvent` signals ("new mail arrived") per user. Payload shape: TBD (read `ChatEventStream`). |
| POST | `/billing/webhook` | Stripe signature | CSRF-exempt; not for clients. |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml` | public | PWA/SEO. |

All other routes return HTML; see `WEB.md`. Errors are rendered via
`GlobalExceptionHandler` / `error.html`, not JSON.

## Planned (TBD — design before implementing)
First mobile feature candidates, in order:
1. `POST /api/v1/auth/token` — exchange email+password for a bearer token (needs new auth mechanism in `SecurityConfig`).
2. `GET /api/v1/chats` — conversation list (backed by `ConversationListService`).
3. `GET /api/v1/chats/{key}` — bubble runs (backed by `ChatService`).
4. `POST /api/v1/chats/{key}/send` — reply (backed by `ReplyService`/`OutboundMessageService`).
Schemas, error codes (planned: `{ "error": code, "message": str }`) are TBD.
