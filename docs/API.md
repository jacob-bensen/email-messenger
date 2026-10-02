# API CONTRACT

Canonical contract between the backend and `conexus-mail-mobile`. **Both repos
must match this file.** Status legend: ✅ implemented · 🟡 planned (contract
proposal, NOT implemented — do not call from the client yet).

Last audited against code: 2026-10-02. Result: **no mobile-facing REST API
exists.** Only the endpoints in §1 return non-HTML.

## 1. Implemented non-HTML endpoints ✅

All require an authenticated browser session (cookie `JSESSIONID`) and, for
POST, a CSRF token (`X-CSRF-TOKEN` header / `_csrf` param) — unusable by the
mobile client as-is (see §2 auth).

| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — messages imported by an immediate poll of the user's active mailboxes | 302 → `/login` if unauthenticated; 403 on bad CSRF |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE). Emits an event on new mail (`NewMailEvent`) for the user. Event name/payload: TBD (see `ChatEventStream`) | 302 → `/login` |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe event JSON | 2xx on accept | `InvalidStripeSignatureException` → error status (TBD exact code) |
| GET | `/actuator/health` | public | none | Spring Boot health JSON | — |

## 2. Planned v1 API 🟡

Base path `/api/v1`, JSON (`application/json`), UTC ISO-8601 / epoch-millis
timestamps (matching existing `*EpochMilli` view-model fields).

**Auth (planned):** `POST /api/v1/auth/login` `{email,password}` →
`200 {accessToken, refreshToken, expiresIn}`; bearer token on all other calls
(`Authorization: Bearer …`); no CSRF for bearer requests. Errors use
`{ "error": "<code>", "message": "<text>" }` with 401 (bad/expired token),
403 (plan/limit — maps `PlanLimitExceededException`), 404, 422 (validation),
429 (login throttle).

| Method | Path | Maps to service | Response |
|---|---|---|---|
| GET | `/api/v1/chats?mailbox=&q=&filter=&page=` | `ConversationListService.list/counts` | `{items:[ConversationListItem], counts, page}` |
| GET | `/api/v1/chats/{key}?mailbox=` | `ChatService` (marks read) | `ChatConversation` |
| POST | `/api/v1/chats/{key}/messages` | `OutboundMessageService` / `ReplyService` | `{messageId}` |
| POST | `/api/v1/chats` | `OutboundMessageService` (new chat) | `{key}` |
| GET | `/api/v1/mailboxes` | `MailAccountService` | `[MailboxView]` |
| GET | `/api/v1/chats/events` | `ChatEventStream` | SSE |

### DTO shapes (derived from existing records; source of truth = the Java records)

`ConversationListItem`: `key:string, title:string, initials:string,
group:bool, memberCount:int, preview:string, lastActivity:ISO-local-datetime,
lastActivityEpochMilli:long|null, unread:bool, threadCount:long,
accounts:string[], senderEmail:string`.

`ChatConversation`: `key, title, initials, members:[ChatMember], threadCount,
runs:[SenderBubbleRun]`. `ChatMember`: `email, displayName, initials, label,
signatureHtml`. `BubbleMessage`: `messageId:long, bodyHtml (sanitized),
sentAt, sentAtEpochMilli, attachments:[{filename,mime,size}], outbound:bool,
replyToMessageId:long|null`. `SenderBubbleRun` JSON layout: TBD — confirm
against `service/SenderBubbleRun.java` when implementing.

Note: `bodyHtml` is server-sanitized HTML; the client must render it in a
restricted HTML widget and never execute scripts. Attachments are metadata
only (bytes are not stored; `blob_ref` null).
