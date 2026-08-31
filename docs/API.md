# REST API contract

This is the canonical contract between the backend (`email-messenger`) and
the Flutter mobile client (`conexus-mail-mobile`). Both sides implement
this document exactly; when it changes, both sides change in the same
commit run.

## Status

**No REST API exists yet.** The backend today ships only Thymeleaf HTML
controllers plus the Stripe webhook. Every endpoint in the "v1" section
below is planned, not implemented.

Route prefix: `/api/v1`
Content type: `application/json; charset=utf-8` unless noted
Errors: JSON envelope `{ "error": { "code": "STRING", "message": "…", "fields": {…}? } }`
Timestamps: RFC 3339 UTC (`2026-08-31T00:00:00Z`)

Once endpoints ship, mark them as **Implemented** and pin the migration/
service they use.

## Auth (planned)

Bearer-token scheme. On login the client receives an opaque access token
(server-side session store; no PII in the token). Token lives in the
`Authorization: Bearer <token>` header.

### `POST /api/v1/auth/login`
Body: `{ "email": string, "password": string }`
200: `{ "token": string, "expiresAt": timestamp, "user": User }`
401: `AUTH_INVALID_CREDENTIALS` · 403: `AUTH_EMAIL_NOT_VERIFIED`
429: `AUTH_RATE_LIMITED`

### `POST /api/v1/auth/register`
Body: `{ "email": string, "password": string, "displayName": string? }`
201: `{ "token": string, "expiresAt": timestamp, "user": User }`
409: `AUTH_EMAIL_TAKEN`

### `POST /api/v1/auth/logout`
Invalidates the caller's token.
204.

### `POST /api/v1/auth/google`
Body: `{ "idToken": string }` (Google ID token from the native Sign-In SDK)
200: `{ "token": string, "expiresAt": timestamp, "user": User }`

### `POST /api/v1/auth/password/forgot`
Body: `{ "email": string }` · 202 (always, to avoid enumeration).

### `GET /api/v1/me`
200: `{ "user": User, "plan": Plan, "mailboxes": [MailboxSummary] }`

## Mailboxes (planned)

### `GET /api/v1/mailboxes`
200: `{ "items": [Mailbox] }`

### `POST /api/v1/mailboxes/imap`
Body: `{ "email": string, "displayName": string?, "imap": ImapConfig, "smtp": SmtpConfig, "password": string }`
201: `{ "mailbox": Mailbox }`
422: `MAILBOX_CONNECT_FAILED` with `fields.imap` / `fields.smtp` explanation.

### `POST /api/v1/mailboxes/{id}/sync`
Kicks a poll cycle now.
202: `{ "startedAt": timestamp }` · 404: `MAILBOX_NOT_FOUND`

### `DELETE /api/v1/mailboxes/{id}`
204.

## Chats (planned)

### `GET /api/v1/chats?cursor=…&limit=…`
Cursor-paginated (server-authoritative order; most-recent-first).
200: `{ "items": [ChatSummary], "nextCursor": string? }`

### `GET /api/v1/chats/{conversationKey}`
200: `{ "chat": ChatDetail }` (includes bubble runs, participants, signature)

### `GET /api/v1/chats/{conversationKey}/messages?before=…&limit=…`
200: `{ "items": [BubbleMessage], "hasMore": bool }`

### `POST /api/v1/chats/{conversationKey}/send`
Body: `{ "body": string, "attachments": [AttachmentRef]? }`
201: `{ "message": BubbleMessage }` · State transitions per
`OutboundMessageService` (Sending → Sent → Delivered).

### `POST /api/v1/chats/{conversationKey}/read`
Marks the conversation as read up to server's latest.
204.

## Live delivery (planned)

### `GET /api/v1/events` (Server-Sent Events)
`Accept: text/event-stream`. Emits `new-mail`, `chat-updated`,
`message-status` events keyed by conversationKey. Mirrors the existing
`ChatEventStream` used by the web PWA.

Push notifications (Web Push / APNs / FCM) are tracked in INTERNAL_TODO.

## DTOs

```
User             { id, email, displayName, avatarUrl?, plan, emailVerified }
Plan             { name: "FREE"|"PRO"|"BUSINESS", renewsAt?, cancelAt? }
Mailbox          { id, email, provider: "IMAP"|"GMAIL_OAUTH",
                   displayName, signature?, lastSyncedAt?, status }
MailboxSummary   { id, email, provider, status }
ImapConfig       { host, port, useSsl }
SmtpConfig       { host, port, useStartTls }
ChatSummary      { conversationKey, participants: [Participant],
                   lastMessageAt, lastMessagePreview, unreadCount,
                   pinned }
ChatDetail       { summary: ChatSummary, threads: [ThreadRef],
                   signatureCard: SignatureCard? }
BubbleMessage    { id, direction: "IN"|"OUT", sender: Participant,
                   sentAt, body, bodyMarkdown, attachments: [Attachment],
                   status: "SENDING"|"SENT"|"DELIVERED"|"FAILED"?,
                   threadId, messageIdHeader }
Participant      { email, displayName?, avatarUrl? }
Attachment       { id, filename, mimeType, sizeBytes, downloadUrl? }
AttachmentRef    { uploadId }   // future: chunked upload
SignatureCard    { phone?, title?, company?, links: [string] }
```

## Versioning

`/api/v1` freezes once the mobile app ships to a public store. Additive
fields are allowed; renames or removals require `/api/v2` and a client
release supporting both.
