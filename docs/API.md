# API contract (canonical — shared with `conexus-mail-mobile`)

_Status 2026-10-05: **no mobile REST API exists yet.**_ The server exposes only
session-cookie, CSRF-protected, server-rendered HTML routes (see `docs/WEB.md`).
This file records (1) what machine-readable surface exists today and (2) the
contract rules every future `/api/v1/**` endpoint must follow. Unimplemented
endpoints are marked **TBD** and must be specified here *before* either repo
implements them.

## 1. Machine-readable surface that exists today

| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` | 302→`/login` if unauthenticated; 403 on missing CSRF |
| GET | `/chats/events` | session | none | SSE stream (`text/event-stream`); event payload shape: TBD (see `ChatEventStream`) | 302→`/login` |
| POST | `/billing/webhook` | Stripe signature header | Stripe event JSON | 200 | 400 invalid signature |
| GET | `/actuator/health` | public | none | Spring Boot health JSON | — |

Everything else returns HTML or redirects and is **not** part of the mobile contract.

## 2. Rules for future `/api/v1/**` (TBD — none implemented)

- JSON only; `Content-Type: application/json`; UTF-8; ISO-8601 UTC timestamps.
- Auth: **TBD.** Current server has only cookie sessions + CSRF. Proposed:
  `POST /api/v1/auth/login {email,password}` → `{accessToken, refreshToken, expiresAt}`
  with `Authorization: Bearer`, a separate stateless filter chain with CSRF off
  for `/api/**`. Needs owner sign-off (Google-only accounts have no password:
  `users.password_set`, V19).
- Error envelope: `{"error": {"code": "<SNAKE_CASE>", "message": "..."}}` with
  401 unauthenticated, 403 forbidden/plan limit (`PLAN_LIMIT_EXCEEDED`), 404, 422 validation.
- Candidate resources (mirroring existing services, **not yet specified**):
  conversations list (`ConversationListService`), conversation detail + mark-read
  (`ChatService`), send/new message (`OutboundMessageService`), mailboxes
  (`MailAccountService`), live events (SSE or push).
- Server is authoritative: client never sends computed fields (conversationKey,
  plan limits, unread counts).
