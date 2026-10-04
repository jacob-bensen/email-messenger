# API.md — canonical API contract

Status 2026-10-04: **no mobile REST API exists yet.** §1 documents the machine-readable
endpoints that exist today; §2 is the *proposed* v1 mobile contract (NOT implemented — the
first feature run should implement it, client and server together). Update this file before
changing either side.

## 1. Existing non-HTML endpoints (session-cookie auth, CSRF required on POST)
| Method/Path | Auth | Response |
|---|---|---|
| `POST /chats/refresh` | session | `200 {"imported": <int>}` — polls the user's mailboxes now |
| `GET /chats/events` | session | `text/event-stream`; new-mail events (`NewMailEvent`); payload schema TBD |
| `POST /billing/webhook` | Stripe signature (no CSRF) | Stripe event ingest |
Errors: unauthenticated -> 302 to `/login` (HTML), not 401 JSON.

## 2. Proposed `/api/v1` (TBD — not implemented)
Auth: `POST /api/v1/auth/login {email,password}` -> `{accessToken, refreshToken, expiresIn}`;
`Authorization: Bearer`. Errors: `{ "error": {"code": "...", "message": "..."} }` with 401/403/404/409/422.
- `GET /api/v1/chats?q=&mailbox=&filter=&page=` -> `{items: ConversationListItem[], counts, page}`
  (fields as `service/ConversationListItem`: key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts,senderEmail)
- `GET /api/v1/chats/{key}?mailbox=` -> `ChatConversation` (key,title,initials,members[],threadCount,runs[]); marks read
- `POST /api/v1/chats/{key}/send {body, subject?, asReply}` -> `201 BubbleMessage`
- `GET /api/v1/mailboxes`, `GET /api/v1/me` (plan, limits) — TBD
Server stays authoritative: plan limits, recipient resolution and signatures are computed server-side.
