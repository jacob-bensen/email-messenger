# API.md — canonical API contract (backend ⇄ mobile client)

Status as of 2026-10-13: **the backend exposes no mobile REST API.** This file
documents (1) what machine-readable endpoints exist today and (2) the *planned*
`/api/v1` surface the mobile client should be built against. Section 2 is a
proposal; nothing in it is implemented. Both repos must change together when it is.

## 1. Existing non-HTML endpoints (implemented)

All require the browser session cookie (`JSESSIONID` / remember-me) and, for POST,
a CSRF token, except where noted.

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — polls the user's due mailboxes now |
| GET | `/chats/events` | session | `text/event-stream`; emits new-mail events (`NewMailEvent` via `ChatEventStream`). Event payload schema **TBD** (see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe webhook; not for clients |
| GET | `/actuator/health` | public | Spring health |

Everything else returns HTML (see `WEB.md`). Errors: unauthenticated → `302 /login`;
CSRF failure → `403`; `GlobalExceptionHandler` renders `error.html`.

## 2. Planned `/api/v1` (NOT IMPLEMENTED — proposal)

Conventions: JSON, `Authorization: Bearer <access token>`, ISO-8601 UTC or epoch-millis
(`...EpochMilli`, matching the existing view records), error body
`{"error":"<code>","message":"<text>"}`. Controllers must call the same services the
web controllers use (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailAccountService`, `PlanLimitService`).

| Method | Path | Maps to service | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `UserService` / Spring auth | `{email,password}` → `{accessToken,refreshToken,expiresAt}`; reuse `LoginThrottleService` |
| POST | `/api/v1/auth/refresh` | new | rotate tokens |
| GET | `/api/v1/me` | `UserService`, `PlanLimitService` | `{email,plan,emailVerified,limits}` |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `ConversationListService` | page of `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts,senderEmail`) + `chatCounts` |
| GET | `/api/v1/chats/{key}?mailbox` | `ChatService` | `ChatConversation`: members (`ChatMember`) + runs (`BubbleRun`→`BubbleMessage`: `messageId,bodyHtml,sentAtEpochMilli,attachments,outbound,replyToMessageId`) |
| POST | `/api/v1/chats/{key}/messages` | `OutboundMessageService` | `{body,subject?,asReply?}` (+ multipart attachments) |
| POST | `/api/v1/chats` | `OutboundMessageService` | `{to[],subject?,body}` new conversation |
| POST | `/api/v1/chats/refresh` | `MailboxPollingService` | `{imported}` |
| GET | `/api/v1/mailboxes` | `MailAccountService` | list connected accounts |
| GET | `/api/v1/chats/events` | `ChatEventStream` | SSE with bearer auth |

Open design questions (TBD): token format/storage and revocation (new table + Flyway
migration), whether to render `bodyHtml` natively or ship plain text, pagination
shape, attachment download (blocked on blob store, see `CLAUDE.md` roadmap), push
notifications (blocked on Web Push roadmap item; mobile would need FCM/APNs).
