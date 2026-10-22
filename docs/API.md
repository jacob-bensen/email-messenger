# API contract (canonical) — ConexusMail

Status: **no mobile REST API exists yet.** This file documents (A) what the server
actually exposes today that is machine-readable, and (B) the planned `/api/v1` surface
as *proposed/TBD* (nothing in B is implemented). Do not code the client against B until
a run moves an endpoint from B to A in the same commit as server + client work.

## A. Implemented, machine-readable endpoints

### POST /chats/refresh
- Auth: authenticated session cookie + CSRF token (`X-CSRF-TOKEN` header / `_csrf`).
- Request: empty body.
- 200: `{"imported": <int>}` — messages newly imported across the user's active mailboxes (synchronous).
- Errors: 302 → `/login` when unauthenticated; 403 on bad/missing CSRF.

### GET /chats/events  (SSE, `text/event-stream`)
- Auth: session cookie. Emits a signal when `MailboxPollingService` imports new mail
  (`NewMailEvent`). Event payload shape: TBD (see `web/ChatEventStream`).

### GET /actuator/health
- Public. Spring Boot health JSON.

### POST /billing/webhook
- Stripe → server only, signature-verified. Not for the client.

All other routes return HTML (see `WEB.md`). Authentication today is session cookie via
`POST /login` (form: `email`, `password`, `remember-me`) with CSRF — unsuitable for a
native client; see section B.

## B. Proposed `/api/v1` (TBD — NOT implemented)
Auth: `POST /api/v1/auth/token` (email+password or Google ID token) → short-lived bearer
access token + refresh token; all other calls `Authorization: Bearer`. Errors:
`{"error":{"code","message"}}`.

| Method & path | Maps to existing service | View model to serialize |
|---|---|---|
| `GET /api/v1/chats?mailbox&q&filter&page` | `ConversationListService.list/counts` | `ConversationListItem` (+ `ChatCounts`) |
| `GET /api/v1/chats/{key}` | `ChatService`/`chatFor` + `markRead` | `ChatConversation` → `BubbleRun` → `BubbleMessage`, `ChatMember` |
| `POST /api/v1/chats/{key}/messages` | `ReplyService` + `OutboundMessageService` | body, subject, asReply |
| `POST /api/v1/chats` | `ReplyService.sendNewEmail` + `OutboundMessageService.recordNewEmail` | to[], subject, body |
| `GET /api/v1/mailboxes` | `MailAccountService.list` | `MailboxView` |
| `POST /api/v1/devices` | (needs Web Push/FCM — roadmap item) | TBD |

Timestamps: UTC epoch millis (`sentAtEpochMilli`, `lastActivityEpochMilli`), matching the
existing view records.
