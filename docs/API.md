# API contract

**Status (2026-10-03): the server exposes NO JSON REST API for the mobile client.** This file is the
canonical contract; §1 documents what exists today, §2 is the *proposed* v1 contract that the next
feature run must implement on both sides (marked PROPOSED — nothing in §2 is implemented).

## 1. Machine-callable endpoints that exist today
All require a session cookie unless stated. All POSTs need a CSRF token (except webhook).

| Method / path | Auth | Request | Response |
|---|---|---|---|
| `POST /login` | public, form (`email`,`password`,`remember-me`) | form + CSRF | 302 to `/chats` (or `/login?error`) |
| `POST /chats/refresh` | session | — | `200 {"imported": <int>}` |
| `GET /chats/events` | session | — | `text/event-stream`; `NewMailEvent` pushes (event shape TBD — read `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature | raw Stripe JSON | 2xx / 400 invalid signature |
| `GET /actuator/health` | public | — | Boot health JSON |

Everything else returns HTML/redirects (see `docs/WEB.md`). Errors: HTML `error.html` via
`GlobalExceptionHandler` (413 for oversized uploads); no JSON error envelope.

## 2. PROPOSED v1 (not implemented)
Base `/api/v1`, JSON, `Authorization: Bearer <accessToken>`. Error envelope
`{"error":{"code":"string","message":"string"}}` with 400/401/403/404/409/413/429.
Shapes mirror existing records (`ConversationListItem`, `ChatConversation`, `BubbleRun`, `BubbleMessage`).
Timestamps: epoch milliseconds UTC (as `lastActivityEpochMilli`).

- `POST /api/v1/auth/token` `{email,password}` → `{accessToken,refreshToken,expiresIn}`; `POST /api/v1/auth/refresh`; `POST /api/v1/auth/logout`. Throttling reuses `LoginThrottleService`. TBD: Google sign-in for mobile.
- `GET /api/v1/chats?q&mailbox&filter=ALL|UNREAD|AWAITING|ATTACHMENTS&page` → `{items:[{key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts[],senderEmail}], counts:{all,unread,awaiting,attachments}, page, pageSize}`
- `GET /api/v1/chats/{key}?mailbox` → `{key,title,initials,members:[{email,displayName,initials,label,signatureHtml}],threadCount,runs:[{sender,outbound,messages:[{messageId,bodyHtml,sentAt,attachments[],outbound,replyToMessageId}]}]}`; marks read (as web does).
- `POST /api/v1/chats/{key}/messages` `{body,subject?,asReply?,mailbox?}` → `201` message; attachments multipart TBD.
- `POST /api/v1/chats` `{to[],subject?,body,mailbox?}` → `201 {key}`.
- `GET /api/v1/mailboxes`, `POST /api/v1/chats/refresh`.
- Live updates: reuse SSE `/chats/events` with bearer auth (TBD).
Server remains authoritative for plan limits (`PlanLimitExceededException` → 403 `plan_limit`), recipients, sender selection, signatures.
