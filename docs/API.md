# API contract (canonical)

**Status: no REST API exists yet (audit 2026-11-09).** The mobile client has no endpoints to call. This file is the home of the contract; the sections below are a *proposal*, all **TBD** until implemented. Nothing here may be assumed live.

## Existing machine-readable endpoints (session-cookie + CSRF, not mobile-ready)
- `POST /chats/refresh` → `200 {"imported": <int>}`
- `GET /chats/events` → `text/event-stream`, new-mail notifications (`NewMailEvent`)
- `POST /billing/webhook` → Stripe only.

## Proposed v1 (TBD — to be designed in a dedicated feature run)
Base `/api/v1`, JSON, `Authorization: Bearer <token>` (new token issuance needed; current auth is form login/session). Errors: `{"error": {"code": "...", "message": "..."}}` with 400/401/403/404/409/429.

| Endpoint | Backed by (existing service) | Mirrors record |
|---|---|---|
| `POST /auth/token` | UserService / Spring auth | — |
| `GET /conversations?mailbox=&q=&filter=&page=` | ConversationListService | `ConversationListItem` |
| `GET /conversations/{key}` | chat service (`ChatConversation`) | `ChatConversation`, `SenderBubbleRun`, `BubbleMessage` |
| `POST /conversations/{key}/read` | chatService.markRead | — |
| `POST /conversations/{key}/messages` | ReplyService | — |
| `GET /mailboxes` | MailAccountService | — |

Rules: server is authoritative; plan limits enforced via `PlanLimitService` (HTTP 402/403 TBD).
