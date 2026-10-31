# API.md — canonical contract

> **Status: there is NO JSON REST API yet.** The server is session-cookie + CSRF, HTML-first. This file (1) documents the machine-readable endpoints that really exist and (2) holds the *proposed* `/api/v1` contract for the mobile client, clearly marked **PROPOSED** (not implemented). The client must not call PROPOSED endpoints until they are marked IMPLEMENTED here.

## 1. Implemented non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | Sync active user's mailboxes now. 200 `{"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream` (SSE); `NewMailEvent` published when polling/IDLE imports mail. Event payload shape: TBD (read `ChatEventStream`). |
| POST | `/billing/webhook` | Stripe-Signature | 200 `ok`; 400 missing/invalid signature; 503 not configured; 500 error. Plain-text bodies. |
| GET | `/actuator/health` | public | Spring Actuator |

All other routes return HTML. Unauthenticated requests redirect (302) to `/login`.

## 2. PROPOSED `/api/v1` (not implemented)
Error envelope: `{"error": {"code": "STRING", "message": "STRING"}}`; 401 unauthenticated, 403 plan-limit (`PLAN_LIMIT`, mirrors `PlanLimitExceededException`), 404, 422 validation.

Auth: `POST /api/v1/auth/login {email,password}` → `{accessToken, refreshToken, expiresIn}`; `POST /api/v1/auth/refresh`; bearer `Authorization: Bearer <token>`; no CSRF on `/api/**`. (Needs a stateless filter chain — TBD design.)

Conversations (shapes derived from existing view records):
- `GET /api/v1/conversations?q=&mailbox=&filter=all|unread|...&page=0` → `{items:[{key,title,initials,group,memberCount,preview,lastActivity(epochMilli),unread,threadCount,accounts[],senderEmail}], counts, page}` (from `ConversationListItem` / `ConversationListService`)
- `GET /api/v1/conversations/{key}` (marks read) → `{key,title,initials,members:[{email,displayName,initials,label,signatureHtml}],threadCount,messages:[{messageId,bodyHtml,sentAt(epochMilli),outbound,replyToMessageId,attachments:[{filename,mime,size}]}]}`
- `POST /api/v1/conversations/{key}/messages {body,subject?,asReply?}` → 201 message. Reuses `ReplyService`.
- `GET /api/v1/events` SSE, same source as `/chats/events`.
Mailboxes, billing and account endpoints: TBD.
