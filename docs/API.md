# API contract (canonical, shared with conexus-mail-mobile)

## Status (2026-10-03): NO MOBILE API EXISTS
The backend only serves server-rendered HTML (session cookie + CSRF). The mobile repo is empty.
Existing non-HTML endpoints (not stable, not for mobile use yet):

| Method/Path | Auth | Response |
|---|---|---|
| `POST /chats/refresh` | session+CSRF | `{"imported": <int>}` |
| `GET /chats/events` | session | SSE new-mail signal |
| `POST /billing/webhook` | Stripe signature | 200/400 |

## Proposed v1 (NOT IMPLEMENTED — design target, to be finalized before coding)
Base path `/api/v1`, JSON, bearer-token auth (TBD: opaque token table vs JWT; Google sign-in via ID token exchange TBD).
Errors: `{"error": {"code": "...", "message": "..."}}` with 400/401/403/404/409/413/429.
The first slice should mirror data the web already computes:

- `POST /api/v1/auth/login` `{email,password}` → `{token, expiresAt}`; `POST /api/v1/auth/logout`
- `GET /api/v1/chats?q&mailbox&filter=ALL|UNREAD|AWAITING|ATTACHMENTS&page` → page of
  `ConversationListItem {key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}` + `counts {all,unread,awaiting,attachments}`
- `GET /api/v1/chats/{key}` → `ChatConversation {key,title,initials,members[{email,displayName,initials,label,signatureHtml}],threadCount,runs[{sender,outbound,messages[{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}]}` (marks read)
- `POST /api/v1/chats/{key}/send` `{body, subject?, asReply?}` (attachments TBD multipart)
- `GET /api/v1/mailboxes`

DTOs map 1:1 to the existing records in `com.emailmessenger.service`. All of it must call the same services as the web
controllers (requires first extracting send logic from `ConversationController`).
