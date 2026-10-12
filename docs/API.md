# API.md — canonical contract between server and mobile client

Last audited: 2026-10-12. **Status: no mobile-facing REST API is implemented.**
Sections: §1 what exists today, §2 the planned `/api/v1` contract (DRAFT — not
implemented; client must not code against it until marked IMPLEMENTED).
Change this file first, then both repos, in the same run.

## 1. Existing machine-oriented endpoints (session cookie + CSRF)
| Method & path | Auth | Response |
|---|---|---|
| `POST /chats/refresh` | session + CSRF token | `200 {"imported": int}` — synchronous mailbox refresh |
| `GET /chats/events` | session | SSE stream (`text/event-stream`) of new-mail events (`ChatEventStream`); event payload shape TBD (read `NewMailEvent`) |
| `POST /billing/webhook` | Stripe signature, no CSRF | Stripe only; not for clients |
| `GET /actuator/health` | none | Spring health |

Every other route returns HTML (see `WEB.md`). Errors are rendered `error.html`
or redirects; there is no JSON error envelope.

## 2. Planned `/api/v1` (DRAFT, NOT IMPLEMENTED)
Auth: stateless bearer token (design TBD — session cookies + CSRF are unsuitable
for a native client). Proposed first slice, mirroring existing services:

| Method & path | Maps to service | Notes |
|---|---|---|
| `POST /api/v1/auth/login` | `UserService`/Spring auth | `{email,password}` → `{accessToken,expiresAt}`; throttled by `LoginThrottleService` |
| `GET /api/v1/chats?mailbox&q&filter&page` | `ConversationListService.list/counts` | `filter` ∈ `ALL,UNREAD,AWAITING,ATTACHMENTS`; page size = `ConversationController.PAGE_SIZE`; item = `ConversationListItem` |
| `GET /api/v1/chats/{key}` | `ChatService.markRead` + `ChatConversation` | `{key,title,initials,members[],threadCount,runs[]}`; bubble = `BubbleMessage` |
| `POST /api/v1/chats/{key}/send` | `ReplyService` + `OutboundMessageService` | `{body, subject?, asReply?}`; attachments multipart (TBD) |

Error envelope (proposed): `{ "error": "CODE", "message": "..." }`, 401 unauth,
403 plan limit (`PlanLimitExceededException`), 404 unknown key, 422 validation.

Prerequisite: extract send/recipient/signature orchestration out of
`ConversationController` into a service (INTERNAL_TODO #2).
