# API contract (canonical, shared with `conexus-mail-mobile`)

**Status 2026-10-27: no mobile REST API is implemented.** The server only exposes the
Thymeleaf website plus the few machine-readable endpoints in §1. Section 2 is a
PROPOSAL (not implemented, not final) so both repos start from one document; the next
feature run must finalize it *before* coding (contract-first), then update this file.

## 1. Machine-readable endpoints that exist today
All except health and webhook use session-cookie auth + CSRF (see `BACKEND.md`).

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/actuator/health` | none | Spring Boot health JSON |
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — triggers an immediate poll of the user's active mailboxes |
| GET | `/chats/events` | session | `text/event-stream`; "new mail" signals (event schema TBD — see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe event ingest; not for clients |

Errors: unauthenticated requests get a 302 to `/login` (not 401 JSON); validation and
plan-limit errors are rendered as HTML by `GlobalExceptionHandler`. TBD: JSON error
envelope.

## 2. PROPOSED v1 mobile API (NOT IMPLEMENTED)
Base path `/api/v1`, JSON, UTF-8. Timestamps: UTC epoch milliseconds (matches the
existing `lastActivityEpochMilli`/`sentAtEpochMilli`). Auth: bearer access token
(short-lived) + refresh token, issued by `POST /api/v1/auth/token`; separate
`SecurityFilterChain` for `/api/**` returning JSON 401/403 and with CSRF off.

Error envelope: `{"error": {"code": "<UPPER_SNAKE>", "message": "<text>"}}`.
Codes: `UNAUTHENTICATED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `VALIDATION` 422,
`PLAN_LIMIT` 402, `RATE_LIMITED` 429.

| Method | Path | Purpose | Backed by (existing service) |
|---|---|---|---|
| POST | `/api/v1/auth/token` | email+password → `{accessToken, refreshToken, expiresIn}` | `DbUserDetailsService`, `LoginThrottleService` |
| POST | `/api/v1/auth/refresh` | rotate tokens | new |
| GET | `/api/v1/conversations?q=&mailbox=&filter=all|unread|...&page=` | list → `ConversationListItem[]` + counts | `ConversationListService` |
| GET | `/api/v1/conversations/{key}` | timeline → `ChatConversation` | `ChatService` |
| POST | `/api/v1/conversations/{key}/messages` | `{body, subject?, asReply?}` | `ReplyService`/`OutboundMessageService` |
| GET | `/api/v1/mailboxes` | connected accounts | `MailAccountService` |
| POST | `/api/v1/devices` | register push token (depends on Web Push/FCM roadmap) | new |

DTO shapes mirror the existing records in `service/` (`ConversationListItem`,
`ChatConversation`, `ChatMember`, `SenderBubbleRun`, `BubbleMessage`); field-by-field
schemas are TBD and will be pinned when the first endpoint is implemented.
