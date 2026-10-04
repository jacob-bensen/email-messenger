# API contract

**Status (2026-10-04): the backend has NO JSON API for the mobile client.** This
file is the canonical contract; §1 documents what exists, §2 is the *draft* v1
contract to be implemented first (contract-first). Nothing in §2 exists yet.

## 1. Existing machine-facing endpoints
| Method/path | Auth | Notes |
|---|---|---|
| `POST /chats/refresh` | session + CSRF | → `200 {"imported": <int>}` |
| `GET /chats/events` | session | SSE stream of new-mail events (`NewMailEvent`); payload shape TBD |
| `POST /billing/webhook` | Stripe signature, CSRF-exempt | Stripe only |

## 2. Draft `/api/v1` (NOT IMPLEMENTED — to be finalized with first feature)
Conventions: JSON, `Authorization: Bearer <accessToken>`, UTC epoch millis for
times, errors `{"error": "<code>", "message": "<text>"}` with 400/401/403/404/409/429.
No CSRF on `/api/**` (bearer auth, stateless).

- `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresInSeconds,user:{id,email,plan}}`; 401 `bad_credentials`, 429 `throttled` (reuse `LoginThrottleService`).
- `POST /api/v1/auth/refresh` `{refreshToken}` → same token pair.
- `GET /api/v1/me` → `{id,email,plan}`.
- `GET /api/v1/chats?mailbox=&q=&filter=ALL|UNREAD&page=` → `{items:[ConversationListItem]}` where item = `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`.
- `GET /api/v1/chats/{key}` → `{key,title,initials,members:[{email,displayName,initials,label,signatureHtml}],threadCount,runs:[…]}`; marks read.
- `POST /api/v1/chats/{key}/messages` `{body,subject?,asReply?}` → `201` message; 402-style `plan_limit` error TBD.

DTOs mirror the existing records in `com.emailmessenger.service` so web and API share one service layer.
