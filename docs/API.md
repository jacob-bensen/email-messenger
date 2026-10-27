# API contract

**Status: the mobile REST API does not exist yet.** Section 1 documents the only
machine-facing endpoints that are implemented today. Section 2 is a *proposal* (not
implemented, `TBD` where undecided); it becomes binding only when a run implements it
in both repos and moves it into section 1.

## 1. Implemented (session-cookie auth, CSRF on unless noted)

| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` synchronous poll of the user's mailboxes |
| `GET /chats/events` | session | none | `text/event-stream`; fires when new mail is imported (event name/payload: TBD, read `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe JSON | 2xx on handled; `InvalidStripeSignatureException` on bad signature (status TBD) |
| `GET /actuator/health` | public | | Spring Boot health |

Errors: unauthenticated requests get a 302 to `/login` (not 401 JSON); `GlobalExceptionHandler`
renders HTML error pages (incl. friendly 413 for oversized uploads: 10MB/file, 25MB/request).

## 2. Proposed `/api/v1` (NOT IMPLEMENTED)

Auth: TBD (proposal: `POST /api/v1/auth/token` email+password → short-lived access token +
refresh token; Google sign-in via native OIDC id-token exchange). CSRF not applicable to Bearer.

Resources mirroring the existing view records:
- `GET /api/v1/chats?q&mailbox&filter=all|unread|…&page` → page of `ConversationListItem`
  `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
- `GET /api/v1/chats/{key}` → `ChatConversation` `{key,title,initials,members[{email,displayName,initials,label,signatureHtml}],threadCount,runs[…]}`; marks read
- `POST /api/v1/chats/{key}/messages` `{body,subject?,asReply,mailbox?}` (+ attachments)
- `POST /api/v1/chats` `{to[],subject?,body}` new conversation
- `GET /api/v1/mailboxes`, `GET /api/v1/me` (plan + limits)
- Live updates: reuse SSE or FCM push (TBD).

Prereq: extract send/compose orchestration out of `ConversationController` (see `BACKEND.md`).
