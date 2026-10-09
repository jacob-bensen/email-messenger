# API contract

**Status (2026-10-09): there is no mobile REST API.** No `/api/**` endpoints exist
and the mobile client has no code. Nothing below is a promise to the client until
it is moved into "Implemented".

## Implemented (non-HTML) endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header (CSRF-exempt) | Raw Stripe event JSON; 400 on bad signature |
| GET | `/chats/events` | session | `text/event-stream` of `NewMailEvent` for the signed-in user |
| GET | `/actuator/health` | none | Spring health |
| GET | `/manifest.webmanifest`, `/sw.js` | none | PWA shell |

All other routes return HTML; see `WEB.md`.

## Planned (proposal, TBD — to be designed in the first feature run)
Token auth (`POST /api/v1/auth/login` → bearer token), `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`. Schemas TBD.
Each must delegate to the same services used by `ConversationController`
(`ConversationListService`, `ChatService`, `OutboundMessageService`).
