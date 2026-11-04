# API.md — canonical API contract

**Status (2026-11-04): NO MOBILE-FACING REST API EXISTS.** The backend only
serves HTML (session cookie + CSRF). This file is the contract the mobile
client will be built against; nothing below "Existing non-HTML endpoints" is
implemented yet. Do not code the client against "Planned" until it moves to
"Implemented".

## Existing non-HTML endpoints (implemented)
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| `GET /chats/events` | session | – | SSE stream (`NewMailEvent`) |
| `POST /billing/webhook` | Stripe signature, no CSRF | Stripe event JSON | 2xx / 400 invalid signature |
| `GET /actuator/health` | public | – | Boot health JSON |

## Planned: `/api/v1` (TBD — proposal, not implemented)
Conventions: JSON, `Authorization: Bearer <access token>`, errors as
`{"error": "<code>", "message": "<text>"}` with 401/403/404/409/422/429.
Each endpoint must call the same service as its web twin.

| Planned | Web twin / service |
|---|---|
| `POST /api/v1/auth/token` (email+password → access+refresh) | `AuthController` / `UserService`; also needs Google sign-in path (TBD) |
| `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` | new token store (TBD) |
| `GET /api/v1/chats?mailbox&q&filter&page` | `ConversationListService.list/counts` |
| `GET /api/v1/chats/{key}` (marks read) | `ChatService` |
| `POST /api/v1/chats/{key}/send` {body, subject?, asReply} | `ConversationController.send` → `ReplyService`/`OutboundMessageService` |
| `GET /api/v1/mailboxes`, `POST …/{id}/sync` | `MailAccountService`, `MailboxPollingService` |
| `GET /api/v1/me` (plan, limits) | `AccountService`, `PlanLimits` |
| push registration | roadmap "Web Push" in CLAUDE.md (TBD) |

DTO schemas: TBD (to be derived from `ConversationListItem`, `ChatConversation`,
`BubbleRun`/`BubbleMessage` records in `service/`).
