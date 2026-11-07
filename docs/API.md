# API.md — canonical contract

**Status (2026-11-07): no mobile REST API is implemented.** This file therefore has two parts:
1. what the server really exposes (HTML/form + two machine endpoints), and
2. the **planned** `/api/v1` contract, which neither repo implements yet. Planned items are
   proposals, not behavior; change them here first, then implement both sides in one run.

## 1. Implemented machine-facing endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe JSON; 400 on bad signature |
| GET | `/chats/events` | session | `text/event-stream`; emits new-mail events (`NewMailEvent`) for the user's mailboxes. Event payload shape **TBD** (read `ChatEventStream`) |
| GET | `/actuator/health` | public | Boot health |

All other routes are Thymeleaf pages — see `docs/WEB.md`. Errors there render `error.html`;
`GlobalExceptionHandler` maps oversized uploads to 413 with a friendly page. There is no JSON error envelope.

## 2. Planned `/api/v1` (NOT IMPLEMENTED)
Conventions: JSON, UTF-8; timestamps as epoch-millis `long` (matches `sentAtEpochMilli` in the view
records); errors `{ "error": "<code>", "message": "<text>" }` with codes `unauthorized`(401),
`forbidden`(403), `not_found`(404), `validation`(422), `plan_limit`(402).

Auth (TBD design): bearer access token + refresh token issued by `POST /api/v1/auth/login`
(`{email,password}`), stateless, no CSRF. Google sign-in for mobile: TBD.

Proposed first slice (read-only), mapped onto existing records:
- `GET /api/v1/me` → `{ id, email, displayName, plan }`
- `GET /api/v1/chats?q=&mailbox=&filter=&page=` → `{ items: ConversationListItem[], page, hasNext }`
  where item = `{ key, title, initials, group, memberCount, preview, lastActivity(epochMs), unread, threadCount, accounts[], senderEmail }`
- `GET /api/v1/chats/{key}?mailbox=` → `ChatConversation` = `{ key, title, initials, members[{email,displayName,initials,label,signatureHtml}], threadCount, runs[{sender, outbound, messages[{messageId, bodyHtml, sentAt(epochMs), outbound, replyToMessageId, attachments[]}]}] }`
- `POST /api/v1/chats/{key}/send` `{ body, subject?, asReply?, mailbox? }` (attachments: TBD multipart)
