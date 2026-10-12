# API.md — canonical API contract (client ↔ server)

**Status (2026-10-12): the server exposes NO REST/JSON API.** There is nothing for the
mobile client to call yet. This file is the contract both repos build against; it is
currently a *draft v1 proposal*, not implemented behavior. Nothing below marked DRAFT
exists in code. Update this file BEFORE implementing any endpoint.

## 1. What exists today (HTML, session+CSRF, not usable by mobile)
See `WEB.md`. Notable non-HTML surfaces:
- `GET /chats/events` — SSE (`text/event-stream`), authenticated by session, emits new-mail signals (`NewMailEvent`). Payload shape: TBD (check `ChatEventStream`).
- `GET /manifest.webmanifest`, `/sw.js`, `/offline`, icons — PWA assets.
- `POST /billing/webhook` — Stripe, signature-verified, no CSRF.
- `GET /actuator/health` — public.

## 2. DRAFT v1 conventions
- Base path `/api/v1`, JSON (`application/json`), UTF-8, timestamps as UTC epoch millis (matches `sentAtEpochMilli`/`lastActivityEpochMilli` in existing view records).
- Auth: DRAFT — `POST /api/v1/auth/login` {email,password} → {accessToken,refreshToken,expiresAt}; `Authorization: Bearer`. Stateless, CSRF not applicable. Google sign-in for mobile: TBD.
- Errors: `{ "error": "<code>", "message": "<text>" }`; codes `unauthorized` 401, `forbidden` 403, `not_found` 404, `validation_failed` 422, `plan_limit_exceeded` 402 (maps `PlanLimitExceededException`/`PlanLimitKind`).

## 3. DRAFT v1 endpoints (first slice: read-only chats)
All require auth unless noted. Shapes mirror existing records so the service layer is shared.

### GET /api/v1/chats?page=0&q=&mailbox=&filter=all
→ `{ "items":[ConversationListItem], "page":0, "hasMore":bool, "counts":{...TBD} }`
`ConversationListItem` = `{key,title,initials,group,memberCount,preview,lastActivity(epochMs),unread,threadCount,accounts[],senderEmail}`.
Backed by `ConversationListService.list/counts` (page size 30). `filter` ∈ values of `ConversationListService.ChatFilter`.

### GET /api/v1/chats/{key}
→ `ChatConversation` = `{key,title,initials,members:[ChatMember],threadCount,runs:[SenderBubbleRun]}`;
`BubbleMessage` = `{messageId,bodyHtml,sentAt(epochMs),attachments[],outbound,replyToMessageId}`.
Backed by `ChatService.buildFor`. Marks read via `ChatService.markRead`. `SenderBubbleRun`/`Attachment` JSON shapes: TBD.

### POST /api/v1/chats/{key}/send  (multipart or JSON) — DRAFT
`{ "body": string≤100000 }` + optional attachments; body OR attachments required. Backed by `ReplyService`/`OutboundMessageService`.

### Not yet specified (TBD, see INTERNAL_TODO.md)
mailbox list/connect/sync, new chat, saved searches, billing/plan, push notifications, live updates for mobile (SSE with bearer vs push).

## 4. Known server gaps blocking the contract
No token auth; no `/api/**` security rules; no JSON controllers; attachment bytes not stored.
