# API.md — canonical API contract

**Status (2026-10-06): there is NO JSON REST API.** The server is a
server-rendered Thymeleaf app. This file records (1) what actually exists and
(2) the proposed v1 mobile API, which is **not implemented** — it is the
contract-first starting point for the next feature run. Both repos must treat
§Proposed as TBD until a run implements it and flips the status below.

## 1. Existing non-HTML surface
| Method & path | Auth | Notes |
|---|---|---|
| `GET /chats/events` | session cookie | `text/event-stream` (SSE). Pushes "new mail" signals for the user (`ChatEventStream`, `NewMailEvent`). Payload schema: TBD (see `ChatEventStream`). |
| `POST /billing/webhook` | Stripe signature header, CSRF-exempt | Stripe events; not for clients. |
| `GET /actuator/health[/**]` | public | health |
| `GET /manifest.webmanifest`, `/sw.js` | public | PWA |

Everything else returns HTML or a redirect and requires a session + CSRF token
(form login at `POST /login`, params `email`, `password`, `remember-me`).
Error pages: `error.html`; `GlobalExceptionHandler` renders HTML.

## 2. Proposed v1 (NOT IMPLEMENTED)
Auth: `POST /api/v1/auth/token` {email,password} → {accessToken, refreshToken, expiresIn}; Bearer on all other calls; CSRF-exempt under `/api/**`. Errors: `{ "error": {"code": string, "message": string} }` with 400/401/403/404/409/429.

| Endpoint | Maps to existing service |
|---|---|
| `GET /api/v1/conversations?page&q&mailbox&filter` | `ConversationListService.list/counts` → `ConversationListItem` |
| `GET /api/v1/conversations/{key}` | `ChatService` → `ChatConversation` / `BubbleRun` / `BubbleMessage` |
| `POST /api/v1/conversations/{key}/messages` {body, subject?, asReply} | `ReplyService` / `OutboundMessageService` |
| `POST /api/v1/conversations` {to, subject, body} | `OutboundMessageService` |
| `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/{id}/sync` | `MailAccountService`, `MailboxPollingService` |
| `GET /api/v1/events` (SSE, Bearer) | `ChatEventStream` |

DTO field lists derive from the records named above (e.g. `ConversationListItem`:
key, title, initials, group, memberCount, preview, lastActivity (epoch ms),
unread, threadCount, accounts[], senderEmail). Finalize before implementing.
