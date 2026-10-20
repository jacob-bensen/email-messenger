# API contract (canonical)

**Status (2026-10-20): there is NO mobile REST API implemented.** The server exposes only server-rendered HTML
plus the JSON/SSE endpoints below. The mobile repo (`conexus-mail-mobile`) is an empty scaffold. This file
documents what exists and a PROPOSED v1 surface that must be agreed here *before* anything is built.

## Implemented JSON/stream endpoints
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — synchronous mailbox refresh |
| GET | `/chats/events` | session | `text/event-stream`, `NewMailEvent` per new mail |
| POST | `/billing/webhook` | Stripe signature header | Stripe event ingest |
| GET | `/actuator/health` | public | Spring Boot health |

Errors: HTML `error` template (404 NoSuchElement/NoResource, 502 mail/billing failures, 413 upload too large).
There is no JSON error envelope yet.

## PROPOSED v1 (not implemented — TBD, subject to change)
Auth: `POST /api/v1/auth/token` (email+password → bearer access/refresh token); `Authorization: Bearer`; no CSRF.
Resources mirroring existing view records:
- `GET /api/v1/chats?page&q&mailbox&filter` → page of `ConversationListItem`
  `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
- `GET /api/v1/chats/{key}` → `ChatConversation` `{key,title,initials,members[{email,displayName,initials,label,signatureHtml}],threadCount,runs[{sender,outbound,messages[{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}]}` (marks read)
- `POST /api/v1/chats/{key}/messages` `{body,subject?,asReply?}` ; `POST /api/v1/chats` `{to[],subject?,body}`
- `GET /api/v1/mailboxes`, `GET /api/v1/me`
Server-side prerequisites are listed in `INTERNAL_TODO.md`.
