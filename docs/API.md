# API — email-messenger

The single source of truth for the contract between the backend and any
non-browser client (starting with `conexus-mail-mobile`). Both repos MUST
match this file exactly. Update this file BEFORE implementing an endpoint
on either side.

## Current status: no JSON API exists yet

Every controller in `com.emailmessenger.web`, `.auth`, `.billing`, `.team`,
`.admin`, `.digest` is Thymeleaf-only (returns HTML or SSE, accepts form
posts). The mobile client cannot use any of them directly. This document
defines the initial JSON API surface. Implementation is not yet done and
is tracked in `INTERNAL_TODO.md`.

## Conventions

- Base path: `/api/v1`
- Auth: HTTP `Authorization: Bearer <token>` header. Tokens are issued by
  `POST /api/v1/auth/login` and are opaque server-issued session tokens
  (revocable via `POST /api/v1/auth/logout`). Tokens are stored server-side
  in a new `mobile_auth_tokens` table.
- Content type: `application/json; charset=utf-8` on both request and
  response bodies unless noted.
- Timestamps: RFC 3339 UTC (e.g. `2026-08-28T14:03:22Z`).
- Errors: HTTP status + JSON body
  `{ "error": "SLUG", "message": "human readable", "field": "optional" }`.
  Slugs: `INVALID_CREDENTIALS`, `NOT_FOUND`, `FORBIDDEN`, `RATE_LIMITED`,
  `VALIDATION`, `SERVER_ERROR`.
- Pagination: `?cursor=<opaque>&limit=<1..100>` returning
  `{ "items": [...], "nextCursor": "..." | null }`. Cursors are opaque.
- CSRF is not required on `/api/v1/**`; tokens are used instead.

## Endpoint inventory (target — v1.0.0 of the contract)

### Auth

- `POST /api/v1/auth/login` — form: `{ email, password }` →
  `{ token, user: User, expiresAt }`. 401 `INVALID_CREDENTIALS`.
- `POST /api/v1/auth/logout` — auth required → `204 No Content`.
- `GET  /api/v1/auth/me` — auth required → `{ user: User }`.
- `POST /api/v1/auth/register` — `{ email, password, displayName? }` →
  `{ token, user }` or 409 `EMAIL_TAKEN`.

### Mailboxes

- `GET  /api/v1/mailboxes` → `{ items: MailAccount[] }`
- `POST /api/v1/mailboxes/imap` — `{ email, host, port, username, password,
  useSsl }` → `MailAccount` (201). Enforces plan limits.
- `POST /api/v1/mailboxes/{id}/sync` → `202 Accepted` — triggers a poll.
- `DELETE /api/v1/mailboxes/{id}` → `204`.

Gmail OAuth stays browser-based: mobile opens the existing
`/mailboxes/gmail/connect` flow in a webview and returns to the app on the
callback deep link.

### Conversations (chats)

- `GET  /api/v1/conversations?cursor=&limit=` →
  `{ items: ConversationSummary[], nextCursor }`
- `GET  /api/v1/conversations/{key}` →
  `{ conversation: Conversation, bubbles: BubbleRun[] }`
- `POST /api/v1/conversations/{key}/read` — `{ upToMessageId? }` → `204`
- `POST /api/v1/conversations/{key}/send` —
  `{ bodyPlain, bodyHtml?, inReplyToMessageId? }` → `Message` (201).
  Server always recomputes threading + sends via SMTP; client's
  `inReplyToMessageId` is a hint only.
- `POST /api/v1/conversations` —
  `{ toEmails: string[], subject, bodyPlain, bodyHtml? }` →
  `{ conversationKey, message }`.

### Search

- `GET /api/v1/search?q=&cursor=&limit=` →
  `{ items: ConversationSummary[], nextCursor }`
- `GET /api/v1/searches` → `{ items: SavedSearch[] }`
- `POST /api/v1/searches` — `{ name, query }` → `SavedSearch` (201)
- `DELETE /api/v1/searches/{id}` → `204`

### Live events

- `GET /api/v1/events` (SSE, `text/event-stream`) — mirrors
  `GET /chats/events`. Emits `newmail` and `chat-updated` events with a
  JSON `data:` payload keyed by `conversationKey`.

### Attachments (roadmap, not yet)

- `GET /api/v1/messages/{id}/attachments/{attachmentId}` — streams bytes.
  Gated on the same plan-limit check as the web download flow, once blob
  storage is wired.

## DTO shapes (v1.0.0)

```json
User             { "id":"long", "email":"string", "displayName":"string?",
                   "plan":"FREE|PERSONAL|TEAM|ENTERPRISE" }

MailAccount      { "id":"long", "email":"string", "authType":"IMAP|GMAIL_OAUTH",
                   "connected":true, "lastPolledAt":"rfc3339?",
                   "unreadThreadCount":"int" }

ConversationSummary { "conversationKey":"string",
                      "participants":[Participant],
                      "lastMessageAt":"rfc3339",
                      "lastMessagePreview":"string",
                      "unreadCount":"int",
                      "threadCount":"int" }

Conversation     { "conversationKey":"string", "participants":[Participant],
                   "subjectLatest":"string" }

BubbleRun        { "senderId":"long", "isOutbound":true, "bubbles":[Bubble] }

Bubble           { "messageId":"long", "sentAt":"rfc3339",
                   "bodyPlain":"string", "bodyHtml":"string?",
                   "attachments":[AttachmentRef],
                   "deliveryState":"SENT|DELIVERED|FAILED?" }

Participant      { "id":"long", "email":"string", "displayName":"string?" }

AttachmentRef    { "id":"long", "filename":"string", "mimeType":"string?",
                   "sizeBytes":"long?" }

SavedSearch      { "id":"long", "name":"string", "query":"string" }
```

## Server authority

Fixed rules — the client MAY send hints but the server is authoritative:

- Threading (`in_reply_to`, `references`, `conversation_key`) is computed
  server-side from message headers. Client `inReplyToMessageId` is a hint
  only.
- Plan limits (mailbox count, thread cap, saved-search cap) are enforced on
  the server; violations return `403 FORBIDDEN` with `error:"PLAN_LIMIT"`.
- Read/unread counts are computed from `email_thread_unread` rows.
- Outbound sending goes through `ReplyService`, which stamps the
  Message-ID; the server-issued value is returned in the response.

## Versioning

The contract version is stored at the top of this file (v1.0.0 today).
Bump the minor for additive changes (new field, new endpoint), the patch
for docs-only, and the major for breaking changes — and in a major, ship
both sides in the same commit.
