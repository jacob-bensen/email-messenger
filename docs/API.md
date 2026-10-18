# API contract (canonical)

**Status 2026-10-18: no mobile REST API exists.** This file records (1) the JSON/stream
endpoints that do exist and (2) the planned `/api/v1` surface, which is **TBD** and must be
specified here *before* any implementation.

## 1. Existing non-HTML endpoints (session cookie + CSRF, browser-oriented)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous poll of the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; emits when `NewMailEvent(userId, importedCount)` fires. Event name/payload: TBD (see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 2xx on handled; `InvalidStripeSignatureException` on bad signature |

Error format for these: not standardized (Spring default `/error`). TBD.

## 2. Planned `/api/v1` (TBD — not implemented)

Prerequisites, in order:
1. Token auth (e.g. `POST /api/v1/auth/login` → access + refresh token, bearer filter,
   CSRF-exempt `/api/**` chain). Decision pending.
2. Standard error envelope `{ "error": { "code": "...", "message": "..." } }`.
3. Read endpoints mirroring chats: conversation list, conversation messages (bubble runs),
   mailbox list.
4. Write endpoints: send message (maps to `OutboundMessageService`), mark read, sync now.
5. Push/live: SSE or Web Push (roadmap in `CLAUDE.md`).

DTOs should be records built from the existing view models (`ConversationListItem`,
`ChatConversation`, `BubbleRun`, `BubbleMessage`, `ChatMember`) — do not expose entities.
