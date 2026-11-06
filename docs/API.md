# API contract (canonical)

> **Status: the mobile REST API does not exist yet.** This file documents (1) what the server exposes today and (2) the planned `/api/v1` contract, marked **PLANNED**. Nothing PLANNED is implemented on either side. Update this file *before* implementing.

## 1. Current non-HTML endpoints (implemented)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` — synchronous poll of the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; emits new-mail signals (`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header | raw Stripe JSON | `200`; invalid signature → error (`InvalidStripeSignatureException`) |

All other routes return HTML/redirects (see `WEB.md`). Errors for HTML routes render `error.html`.

## 2. PLANNED `/api/v1` (mobile) — TBD, to be finalized by the first feature run
Principles: server authoritative; JSON; same services as the website; errors as `{"error": {"code": str, "message": str}}`.

- **Auth** (needs a decision: bearer token issued by `POST /api/v1/auth/token {email,password}` vs session cookie + CSRF; Google sign-in for mobile TBD). Not designed yet.
- `GET /api/v1/chats?q&mailbox&filter(all|unread|…)&page` → page of `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail`).
- `GET /api/v1/chats/{key}?mailbox` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[{sender,outbound,messages[{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}]`); marks read.
- `POST /api/v1/chats/{key}/messages {body, subject?, asReply?, mailbox?}` (attachments multipart) → sent message.
- `POST /api/v1/chats {to[], subject?, body, mailbox?}` → `{key}`.
- `GET /api/v1/mailboxes`, `POST /api/v1/devices` (push token; depends on roadmap Web Push/FCM work).

Field shapes above are copied from the existing view records in `com.emailmessenger.service`; they are the candidate DTOs.
