# API.md — canonical API contract

> **STATUS (2026-10-04): no mobile REST API is implemented.** The backend is a
> server-rendered web app with cookie-session auth + CSRF. The mobile repo
> (`conexus-mail-mobile`) contains only a README. This file records (A) the
> machine-readable endpoints that exist today and (B) the contract v1 target.
> Nothing in (B) exists yet; implement it contract-first per the sync routine.

## A. Non-HTML endpoints that exist today
| Method & path | Auth | Response |
|---|---|---|
| `POST /chats/refresh` | session + CSRF | `200 application/json` `{"imported": <int>}` — synchronously polls the user's active mailboxes |
| `GET /chats/events` | session | `text/event-stream` (SSE) of new-mail events from `ChatEventStream` per user; event payload shape TBD (see `NewMailEvent`) |
| `POST /billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event intake; no client use |
| `GET /actuator/health` | none | Spring Boot health |
Errors today: `GlobalExceptionHandler` renders HTML error views (404 for
`NoSuchElementException`, mail/billing/plan-limit flashes). No JSON error body.

## B. Contract v1 — PLANNED, NOT IMPLEMENTED
Base `/api/v1`, JSON, UTC ISO-8601 or epoch-millis (existing view records use
epoch millis: keep `sentAtEpochMilli`). Auth: Bearer access token (to be
designed — TBD: login endpoint, refresh, Google sign-in for mobile).
Error body: `{"error": {"code": "<SNAKE>", "message": "..."}}`.

Planned first slice (read-only), mirroring existing records:
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` → `[ConversationListItem]`
  `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
- `GET /api/v1/chats/{key}` → `ChatConversation`
  `{key,title,initials,members[{email,displayName,initials,label,signatureHtml}],threadCount,runs[{sender,outbound,messages[{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}]}]}`
  (marks read, as the web pane does)
- Later: `POST /api/v1/chats/{key}/send`, mailboxes, push registration.
