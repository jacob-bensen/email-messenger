# API contract (shared with `conexus-mail-mobile`)

**Status: the server exposes NO JSON REST API today.** The mobile client cannot talk to the
backend yet. Everything below §1 is *current reality*; §2 is the *PROPOSED* v1 contract, to be
implemented next and not yet binding. When implemented, change "PROPOSED" to "LIVE" here in the
same commit.

## 1. Current reality (HTML/session only)
- Auth: form login `POST /login` (`email`, `password`, optional `remember-me`) → 302 + `JSESSIONID`
  cookie; CSRF token required on every non-GET (exempt: `/billing/webhook`).
- Mutations return redirects/HTML. The only non-HTML responses: SSE `GET /chats/events`
  (`text/event-stream`, newline "new mail" signal, see `ChatEventStream`) and PWA assets.
- Errors: `GlobalExceptionHandler` renders `error.html`; upload over limit → 413 page.
- Full route list: `docs/WEB.md`.

## 2. PROPOSED `/api/v1` (mobile)
Conventions: JSON, UTF-8; timestamps = UTC epoch millis (matches `sentAtEpochMilli`); errors
`{ "error": "<code>", "message": "..." }` with 400/401/403/404/409/413/429. Auth: bearer token
(`Authorization: Bearer <token>`), no CSRF; token issuance TBD (see INTERNAL_TODO).

| Method | Path | Purpose | Backed by |
|---|---|---|---|
| POST | `/api/v1/auth/token` | `{email,password}` → `{token,expiresAt}` | `UserService` / auth |
| GET | `/api/v1/mailboxes` | list connected accounts | `MailAccountService` |
| GET | `/api/v1/chats?page&q&mailbox&filter` | `{items:[ConversationListItem],page,hasNext}` | `ConversationListService` |
| GET | `/api/v1/chats/{key}?mailbox` | `ChatConversation` (key,title,initials,members[],threadCount,runs[]) | `ChatService` |
| POST | `/api/v1/chats/{key}/messages` | `{body,subject?,asReply?}` (+attachments) → created message | `OutboundMessageService` |
| POST | `/api/v1/chats` | `{to,subject,body,mailbox?}` new conversation | `OutboundMessageService` |
| GET | `/api/v1/chats/events` | SSE new-mail signal (token auth) | `ChatEventStream` |

DTO shapes mirror the existing records: `ConversationListItem` (key,title,initials,group,
memberCount,preview,lastActivity→epoch,unread,threadCount,accounts[],senderEmail),
`ChatMember` (email,displayName,initials,label,signatureHtml), `BubbleRun` (sender,messages[],
outbound), `BubbleMessage` (messageId,bodyHtml,sentAt→epoch,attachments[],outbound,
replyToMessageId). Fields marked TBD where the record type is not yet confirmed as serializable
(`Attachment` entity must NOT be serialized directly — needs a DTO).
