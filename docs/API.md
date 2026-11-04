# API contract (canonical)

**Status (2026-11-04): no mobile/REST API is implemented.** Nothing below `/api/v1` exists in code. This file lists what machine-facing endpoints do exist and the *planned* v1 surface. Planned items are marked PLANNED and must be finalised here **before** being implemented in either repo.

## Implemented machine-facing endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/billing/webhook` | Stripe `Stripe-Signature` header, no CSRF | raw Stripe event JSON | `200 "ok"`; `400` missing/invalid signature; `503` webhook secret not configured; `500` apply failed (Stripe retries) |
| POST | `/chats/refresh` | session cookie + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session cookie | none | `text/event-stream`; emits new-mail signals (`NewMailEvent`) |

All other routes return HTML (see `WEB.md`) and use session cookies + CSRF.

## PLANNED `/api/v1` (TBD — to be specified)
Needed for the mobile client, mirroring what the web chat workspace already does through services:
- Auth: token login/refresh/logout (TBD: bearer JWT vs. opaque tokens; Google sign-in for mobile).
- `GET /api/v1/chats?q&mailbox&filter&page` → page of `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts,senderEmail`).
- `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[]` of `BubbleRun` → `BubbleMessage{messageId,bodyHtml,sentAtEpochMilli,attachments,outbound,replyToMessageId}`); marks read.
- `POST /api/v1/chats/{key}/messages` (body, optional subject, asReply, attachments).
- `GET /api/v1/mailboxes`, `POST /api/v1/devices` (push token; depends on roadmap "Web Push"/push item).
- Error envelope: TBD (suggest `{ "error": code, "message": text }`; plan-limit failures map from `PlanLimitExceededException`).
