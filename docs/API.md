# API.md — canonical API contract

**Status (2026-10-16): NO REST/JSON API EXISTS.** The backend is a
server-rendered Thymeleaf app using session cookies + CSRF; its HTML routes are
NOT a stable contract and the mobile client must not scrape them. The only
machine-oriented endpoints today:

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE stream of new-mail events (`ChatEventsController`); payload shape TBD (document from `NewMailEvent`) |
| POST | `/chats/refresh` | session + CSRF | returns `{"imported": <int>}` |
| POST | `/billing/webhook` | Stripe signature | not for clients |
| GET | `/actuator/health` | none | Boot health |

## Planned `/api/v1` (PROPOSED — not implemented; finalise before coding)
Shape derived from existing view records so web and API share services:

- **Auth:** TBD. Needs a non-cookie scheme (bearer access token + refresh) — web
  CSRF/session chain must stay untouched. Google sign-in via native OIDC id-token exchange.
- `GET /api/v1/chats?q=&mailbox=&filter=&page=` → page of `ConversationListItem`
  `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
- `GET /api/v1/chats/{key}` → `ChatConversation` (members `{email,displayName,initials,label,signatureHtml}`; bubble runs
  `{sender, outbound, messages[{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}`); marks read.
- `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` (+attachments multipart)
- `POST /api/v1/chats` `{to[], subject?, body}`
- `GET /api/v1/mailboxes`, `POST .../{id}/sync`
- Errors: `{ "error": "<code>", "message": "..." }`; 401 unauth, 402/403 plan limit (`PlanLimitKind`), 404, 413 upload too large, 422 validation.

Timestamps are UTC epoch millis (matches existing view records).
