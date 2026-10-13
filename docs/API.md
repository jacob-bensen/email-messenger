# API contract (canonical)

Status legend: **LIVE** = implemented today; **PLANNED** = agreed shape, not implemented
(client must not call it). Anything unknown is marked TBD. Both repos depend on this file;
change it before changing either implementation.

## Auth (LIVE, browser-oriented)
- `POST /login` form `email`, `password`, optional `remember-me`; needs CSRF token; sets
  `JSESSIONID`. Failure → redirect `/login?error`. Throttled per email/IP.
- `POST /logout`. `GET /auth/google/start` begins Google OIDC.
- All non-public routes: unauthenticated → 302 `/login` (no 401 JSON).

## JSON/stream endpoints that exist today
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — synchronously polls the user's active mailboxes |
| GET | `/chats/events` | session | SSE (`text/event-stream`); new-mail events from `ChatEventStream` (event payload schema TBD — read `ChatEventStream`/`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event ingest; not for clients |
| GET | `/actuator/health` | public | Spring Boot health |

Errors: HTML error page (`error.html`); upload > limits → 413 via `GlobalExceptionHandler`.
There is no standard JSON error envelope yet.

## PLANNED `/api/v1` (mobile client) — not implemented
Bearer-token auth (`POST /api/v1/auth/login` → `{accessToken, refreshToken, expiresIn}`,
`POST /api/v1/auth/refresh`) with CSRF-exempt `/api/**`; errors as
`{"error": {"code": "...", "message": "..."}}` with 401/403/404/409/422/429.

Resources mirroring existing view models (field names = the Java records):
- `GET /api/v1/conversations?page&q&mailbox&filter=ALL|UNREAD|AWAITING|ATTACHMENTS` →
  `{items:[ConversationListItem], page, hasNext, counts:{all,unread,awaiting,attachments}}`
  where `ConversationListItem = {key,title,initials,group,memberCount,preview,
  lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`.
- `GET /api/v1/conversations/{key}?mailbox` (also marks read) → `ChatConversation =
  {key,title,initials,members:[ChatMember{email,displayName,initials,label,signatureHtml}],
  threadCount,runs:[{sender,outbound,messages:[BubbleMessage{messageId,bodyHtml,
  sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}]}`.
- `POST /api/v1/conversations/{key}/messages` `{body, subject?, asReply?}` (+ attachments
  multipart, TBD) → `201` message. Server decides sender account, signature, recipients.
Timestamps: UTC epoch millis. First feature to implement: see INTERNAL_TODO.md.
