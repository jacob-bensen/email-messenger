# API contract

**Status (2026-10-22): no client REST API is implemented.** This file is canonical for both
repos; sections marked PLANNED are the agreed target, not behavior. Update this file
*before* implementing anything under `/api/v1`.

## 1. Implemented machine endpoints (session-cookie auth, CSRF where noted)
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — messages newly imported for the user's active mailboxes (synchronous). |
| `GET /chats/events` | session | none | `text/event-stream`; emits an event when `NewMailEvent` is published for the user. Event payload shape: TBD (read `ChatEventStream`). |
| `POST /billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event JSON | `200` on success; invalid signature -> error via `InvalidStripeSignatureException` (exact status TBD). |
| `GET /actuator/health` | public | — | Spring Boot health. |

Errors: `GlobalExceptionHandler` renders HTML (`error.html`); upload-too-large -> 413.
No JSON error envelope exists.

## 2. PLANNED: `/api/v1` for the mobile client (not implemented)
Decisions to make before building (recorded in `INTERNAL_TODO.md`): token auth scheme
(short-lived access + refresh token, issued by `POST /api/v1/auth/login`; Google sign-in via
ID-token exchange), JSON error envelope `{ "error": {"code","message"} }`.

Proposed first slice (read-only + send), all backed by existing services:
- `POST /api/v1/auth/login` `{email,password}` -> `{accessToken,refreshToken,expiresIn}`
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` -> page of `ConversationListItem`
- `GET /api/v1/chats/{key}` -> `ChatConversation` (members, runs of `BubbleMessage`)
- `POST /api/v1/chats/{key}/send` `{body, subject?, asReply?}` -> created message

DTO source of truth until then: `service/ChatConversation`, `BubbleMessage`,
`ConversationListItem` records.
