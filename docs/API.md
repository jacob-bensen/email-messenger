# API contract (canonical for backend + mobile)

Status: **there is no versioned JSON API yet.** This file records (1) what is
actually exposed today and (2) the planned `/api/v1` contract the mobile client
will target. Section 2 is a PROPOSAL — not implemented on either side. Update
this file first when implementing.

## 1. Implemented today (non-HTML endpoints)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | `text/event-stream` of new-mail signals (payload shape TBD — see `ChatEventStream`/`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 200 / 400 on bad signature |
| GET | `/actuator/health` | public | — | Spring Actuator health |
| GET | `/manifest.webmanifest`, `/sw.js` | public | — | PWA assets |

All other routes return HTML; see `WEB.md`.

## 2. PLANNED `/api/v1` (proposal, TBD)
Conventions: JSON, UTF-8; timestamps as UTC epoch millis (matches existing
`*EpochMilli` view-model accessors); errors as
`{"error": "<code>", "message": "<text>"}` with 400/401/403/404/409/429.

Auth (needs design — TBD): `POST /api/v1/auth/login {email,password}` →
`{accessToken, refreshToken, expiresAt}`; bearer `Authorization` header;
CSRF not applicable to bearer requests.

| Method | Path | Maps to service | Response (sketch) |
|---|---|---|---|
| GET | `/api/v1/chats?mailbox=&filter=&q=&page=` | `ConversationListService` | `[ConversationListItem]`: `key,title,initials,group,memberCount,preview,lastActivity(epoch ms),unread,threadCount,accounts[],senderEmail` |
| GET | `/api/v1/chats/{key}` | `ChatService`/`ConversationService` | `ChatConversation`: `key,title,initials,members[],threadCount,runs[]` (bubble: `messageId,bodyHtml,sentAt,attachments[],outbound,replyToMessageId`); marks read |
| POST | `/api/v1/chats/{key}/messages` | `OutboundMessageService` | `{body, subject?, asReply?, mailbox?}` → created message |
| POST | `/api/v1/chats/refresh` | `MailboxPollingService` | `{imported}` |
| GET | `/api/v1/mailboxes` | `MailAccountService` | connected accounts |
