# API.md — canonical API contract

Status legend: **[LIVE]** implemented today; **[PLANNED]** agreed shape, not implemented (client must not call yet).
Last audited: 2026-10-07. **Finding: the server has no mobile-ready API.** Every route is a server-rendered,
cookie-session, CSRF-protected web route. Only the items in §1 return non-HTML.

## 1. Machine-readable endpoints that exist today [LIVE]
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": int}` — synchronously polls the user's mailboxes |
| `GET /chats/events` | session | none | `text/event-stream` (SSE); event name/payload **TBD** (see `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header | Stripe JSON | 200/400 (server-to-server only) |
| `GET /actuator/health` | public | — | Spring Boot health JSON |
| `GET /manifest.webmanifest`, `/sw.js` | public | — | PWA assets |

Errors on all other routes are HTML pages (`GlobalExceptionHandler`): 404 missing, 413 upload too large (>10MB file / 25MB request),
502 mail/IMAP/SMTP failure, billing errors, `PlanLimitExceededException` → redirect to upgrade modal; IllegalArgument → 400 page.

## 2. Mobile API v1 [PLANNED]
Base `/api/v1`, JSON, UTF-8. Timestamps = UTC epoch millis (matches existing `*EpochMilli()` view helpers).
Errors: `{"error":{"code":"STRING","message":"..."}}` with codes `UNAUTHENTICATED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404),
`VALIDATION`(400), `PLAN_LIMIT`(402, includes `kind`,`current`,`limit`), `UPSTREAM_MAIL`(502), `PAYLOAD_TOO_LARGE`(413).
Auth: **TBD** — needs a token scheme (short-lived access + refresh) since web uses session+CSRF. Proposed:
`POST /api/v1/auth/login {email,password} → {accessToken, refreshToken, expiresAt}`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`.
Google sign-in for mobile: TBD (ID-token exchange).

Shapes below mirror existing service records so the API and web share one service layer.

- `GET /api/v1/chats?page=0&q=&mailbox=&filter=all|unread|awaiting|attachments` →
  `{items:[ConversationListItem], page, hasNext, counts:{all,unread,awaiting,attachments}}`
  - `ConversationListItem`: `{key,title,initials,group,memberCount,preview,lastActivity(epochMs),unread,threadCount,accounts:[string],senderEmail}`
  - Page size: same as `ConversationController.PAGE_SIZE`.
- `GET /api/v1/chats/{key}?mailbox=` → `ChatConversation` and marks the chat read:
  `{key,title,initials,members:[{email,displayName,initials,label,signatureHtml}],threadCount,runs:[Run]}`
  - `Run`: `{sender:{email,displayName,initials},thread:{ThreadRef TBD},outbound,messages:[{messageId,bodyHtml,sentAt,attachments:[{filename,mime,size}],outbound,replyToMessageId}]}`
  - `bodyHtml` is server-sanitized; client must render in a sanitizing/limited HTML widget.
- `POST /api/v1/chats/{key}/messages` (multipart: `body`, `subject?`, `asReply?`, `mailbox?`, `attachments[]`) → 201 `BubbleMessage`.
- `POST /api/v1/chats` `{to:[email], subject?, body, mailbox?}` → 201 `{key}` (new conversation).
- `POST /api/v1/chats/refresh` → `{imported:int}`.
- `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes` (IMAP: `host,port,ssl,username,password,provider?`), `POST /api/v1/mailboxes/{id}/sync`, `DELETE /api/v1/mailboxes/{id}` — plan-limit enforced server-side.
- `GET /api/v1/me` → `{email, plan, emailVerified, limits:{mailboxes,threads,savedSearches}}`.
- Live updates: SSE `/api/v1/chats/events` (token auth) or push (TBD; roadmap "Web Push").

**Rule:** the server recomputes everything (conversationKey, plan limits, read state); the client never supplies them except the opaque `key` it received.
