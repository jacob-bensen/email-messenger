# API contract

## Status: NO MOBILE REST API EXISTS YET

The backend exposes only server-rendered Thymeleaf pages (`WEB.md`), session-cookie + CSRF auth, one Stripe webhook and one SSE stream. There is no `/api/**` namespace, no JSON DTOs, no token auth, and the Flutter client (`conexus-mail-mobile`) contains only a README. Nothing below is implemented; it is the agreed starting point for the first API feature.

## Existing machine-readable endpoints
| Method/path | Auth | Notes |
|---|---|---|
| POST `/billing/webhook` | public, Stripe signature header, CSRF-exempt | Stripe events; not for the mobile client |
| POST `/chats/refresh` | session + CSRF | returns `{"imported": <n>}` |
| GET `/chats/events` | session | `text/event-stream`, new-mail events (`ChatEventStream`) |

## Planned `/api/v1` (PROPOSED — TBD, not implemented)
Principles: JSON, errors as `{"error":{"code","message"}}`, server authoritative, controllers share services with the web layer (`ConversationListService`, `ChatService`, `OutboundMessageService`).

1. Auth: `POST /api/v1/auth/token` (email+password → bearer token), stateless filter chain under `/api/**` with CSRF off. Auth type TBD (needs a token table migration).
2. `GET /api/v1/chats?filter&q&mailbox&page` → list of conversation summaries.
3. `GET /api/v1/chats/{key}` → bubbles (runs), participants, unread state; marks read.
4. `POST /api/v1/chats/{key}/send` → `{body, subject?, asReply?}`.
5. `GET /api/v1/mailboxes`.

Field-level schemas to be written when feature 2 starts (contract first).
