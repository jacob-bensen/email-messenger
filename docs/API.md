# API.md — canonical API contract

**Status: no mobile/JSON API is implemented.** Nothing below `/api` exists. The mobile client cannot authenticate (session+CSRF only). Everything marked TBD must be designed in a future run, contract-first.

## Existing non-HTML endpoints (implemented)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | Triggers mailbox refresh; returns body via @ResponseBody (shape: TBD, read `ConversationController`) |
| GET | `/chats/events` | session | SSE stream of `NewMailEvent`s (`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe events |

## Planned for mobile (TBD — proposal only, not implemented)
- Token auth: `POST /api/v1/auth/login`, `/refresh`, `/logout` (bearer tokens; stateless filter chain separate from the web chain, CSRF-exempt).
- `GET /api/v1/chats` (paged list; same data as `ConversationListService.list`), `GET /api/v1/chats/{key}` (bubbles from `ChatService`), `POST /api/v1/chats/{key}/send` (via `ReplyService`/`OutboundMessageService`), `GET /api/v1/chats/events` (SSE).
- Errors: JSON `{code,message}` — TBD.
