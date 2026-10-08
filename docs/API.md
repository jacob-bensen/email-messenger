# API contract (canonical)

**Status (2026-10-08): no mobile REST API exists.** The server exposes only
session-authenticated, CSRF-protected, HTML (Thymeleaf) routes — see `docs/WEB.md` —
plus the SSE stream `GET /chats/events` and Stripe's `POST /billing/webhook`.
The Flutter client calls nothing yet. This file is the place the contract is
defined *before* implementation (contract-first). Nothing below is invented
behavior; everything else is TBD.

## Existing machine-facing endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header | CSRF-exempt, raw JSON; not for the client |
| GET | `/chats/events` | session cookie | `text/event-stream`; emits new-mail events (`NewMailEvent`); payload schema TBD (document from `ChatEventStream`) |

## Planned `/api/v1` (TBD — not implemented)
Conventions to adopt when built: JSON only; bearer-token auth (session cookies/CSRF
do not suit mobile); errors as `{ "code": string, "message": string }` with
401/403/404/409/422; services reused from the web layer (`ChatService`,
`ConversationListService`, `ReplyService`, `MailAccountService`).

Proposed first slice (see INTERNAL_TODO #1):
- `POST /api/v1/auth/token` — email+password → access token — TBD
- `GET /api/v1/chats?mailbox&filter&q&page` — conversation list — TBD
- `GET /api/v1/chats/{key}` — conversation with bubble runs — TBD
- `POST /api/v1/chats/{key}/send` — send/reply — TBD
