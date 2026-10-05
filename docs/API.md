# API contract

**Status: no mobile REST API exists yet.** Nothing below is implemented; this file is the canonical place to
define it before code is written. Existing JSON-ish/non-HTML endpoints:

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE stream; `NewMailEvent` published by `MailboxPollingService` (payload shape TBD — read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe events; CSRF-exempt |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml`, `/icons/*` | public | PWA/SEO |

## Proposed v1 (TBD, to be finalized when the first feature is built)
- Auth: `POST /api/v1/auth/token` (email+password → bearer token); Google sign-in via ID-token exchange.
- `GET /api/v1/chats` (list), `GET /api/v1/chats/{key}` (bubble runs), `POST /api/v1/chats/{key}/messages`.
- Errors: JSON `{ "error": "<code>", "message": "..." }` with 400/401/403/404/409/429.
- Controllers must call the same services as the web layer (`ConversationListService`, `ChatService`, `ReplyService`).
