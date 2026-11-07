# API contract (canonical)

**Status (2026-11-07): there is no mobile REST API.** The server is
session-cookie + CSRF + Thymeleaf. Nothing below under "PLANNED" exists in
code. Both repos must treat this file as the source of truth; any endpoint
implemented MUST be moved from PLANNED to IMPLEMENTED in the same commit.

## IMPLEMENTED (non-HTML endpoints that exist today)

| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` | 302→/login if unauthenticated; 403 CSRF |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE), signals new mail for the user (`NewMailEvent`); event `new-mail`, data `{"imported": int}` | 302→/login |
| POST | `/billing/webhook` | Stripe signature header, CSRF exempt | Stripe event JSON | 2xx on handled; invalid signature rejected (`InvalidStripeSignatureException`) | 400-class on bad signature (exact code TBD) |
| GET | `/actuator/health` | public | | Spring health JSON | |

Errors for HTML pages are rendered by `GlobalExceptionHandler`; there is no
JSON error envelope yet.

## PLANNED — `/api/v1` (NOT IMPLEMENTED; proposal for next runs)

Conventions to adopt: JSON, `Authorization: Bearer <access token>`, stateless
(separate `SecurityFilterChain` for `/api/**`, CSRF off), error envelope
`{"error":{"code":"...","message":"..."}}`, timestamps as UTC epoch millis
(matches existing `*EpochMilli()` view helpers).

1. `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresAt}`
2. `POST /api/v1/auth/refresh`
3. `GET  /api/v1/chats?page&q&mailbox&filter` → page of `ConversationListItem`
   (`key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts,senderEmail`)
4. `GET  /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[]` with `BubbleMessage`)
   and marks read
5. `POST /api/v1/chats/{key}/messages` `{body,subject?,asReply?}` (attachments TBD)
6. `GET  /api/v1/mailboxes`
7. `POST /api/v1/chats/refresh`

Order of work: auth → read-only list/detail → send. Dependencies are listed in
`INTERNAL_TODO.md`.
