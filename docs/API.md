# API contract (canonical, shared with `conexus-mail-mobile`)

## Status: NO REST API IS IMPLEMENTED YET

Audit 2026-11-04: the server exposes only Thymeleaf pages (see `WEB.md`) with
cookie-session + CSRF auth. The single JSON endpoint is
`POST /chats/refresh` → `{"imported": <int>}` (session auth, CSRF token required).
The mobile client therefore cannot talk to the server today.

Everything below is a **proposal (TBD, not implemented)** — the intended shape
of `/api/v1`, to be finalised and implemented in the first feature run, in the
same commit as client + server code.

## Proposed conventions (TBD)
- Base path `/api/v1`, JSON, UTF-8, ISO-8601 / epoch-millis timestamps (TBD: pick one; web uses UTC epoch millis).
- Auth: bearer access token (+ refresh token) issued by `POST /api/v1/auth/login`; CSRF not required for bearer requests. Google sign-in for mobile: TBD.
- Errors: `{"error": "<code>", "message": "<text>"}` with 400/401/403/404/409/429; plan-limit failures reuse `PlanLimitExceededException` → 403 `plan_limit`.

## Proposed first endpoints (TBD)
| Method | Path | Backed by service |
|---|---|---|
| POST | /api/v1/auth/login, /auth/refresh, /auth/logout | UserService / new token service |
| GET | /api/v1/chats?q&mailbox&filter&page | ConversationListService → `ConversationListItem` |
| GET | /api/v1/chats/{key} | ChatService → `ChatConversation` |
| POST | /api/v1/chats/{key}/send | ReplyService / OutboundMessageService |
| POST | /api/v1/chats/{key}/read | ChatService.markRead |
| GET | /api/v1/mailboxes | MailAccountService |

Existing view-model records that would be the response bodies:
`ConversationListItem(key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail)`,
`ChatConversation(key,title,initials,members,threadCount,runs)`.
