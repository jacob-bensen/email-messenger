# API contract (canonical)

Status 2026-10-28: **no REST API for the mobile client exists.** The mobile repo
(`conexus-mail-mobile`) contains only a README. This file records (1) what is
actually exposed today and (2) the PROPOSED v1 mobile API, marked TBD, which must
be agreed here before either repo implements it.

## 1. Implemented today (session-cookie auth, CSRF required on POST)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | SSE stream (`NewMailEvent`; event payload shape TBD, see `ChatEventStream`) |
| GET | `/actuator/health` | none | Spring Boot health |
| POST | `/billing/webhook` | Stripe signature | Stripe events |
All other routes return HTML (see WEB.md). Errors: unauthenticated -> 302 `/login`.

## 2. PROPOSED v1 (TBD, not implemented)
Base `/api/v1`, JSON, bearer-token auth (new; token issue/refresh design TBD), no CSRF.
Reuse existing records as DTO sources: `ConversationListItem`, `ChatConversation`,
`ChatMember`, `BubbleMessage`.
- `POST /api/v1/auth/login` {email,password} -> {accessToken,refreshToken,expiresIn}
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` -> list of ConversationListItem
- `GET /api/v1/chats/{key}` -> ChatConversation (marks read)
- `POST /api/v1/chats/{key}/send` {body, subject?, asReply} -> 201
- `GET /api/v1/mailboxes`, `GET /api/v1/events` (SSE)
Error envelope TBD: `{ "error": "<code>", "message": "..." }`.
