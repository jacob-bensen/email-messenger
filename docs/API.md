# API contract

Canonical contract between the backend and the mobile client (`conexus-mail-mobile`).
**Status: no mobile REST API is implemented yet.** This file documents (1) the
machine-consumable endpoints that exist today and (2) the planned `/api/v1` surface,
marked *Planned*. Anything marked *Planned* must not be called by the client until
implemented and moved to the "Implemented" section in the same commit.

## Implemented (non-HTML) endpoints

Auth for all: browser session cookie + CSRF token (`_csrf` / `X-CSRF-TOKEN`), unless noted.

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; events published by `ChatEventStream` when `NewMailEvent` fires (payload shape TBD — see `fragments/live-refresh.html`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event JSON | `200` on handled, `400` on invalid signature |
| GET | `/actuator/health` | public | — | Spring Boot health JSON |

Errors: unauthenticated → `302 /login` (HTML flow; no JSON 401 yet); plan-cap violations
raise `PlanLimitExceededException` (rendered as an upgrade modal in HTML; JSON shape TBD).

## Planned `/api/v1` (not implemented)

Design rules: JSON only; stateless bearer-token auth; errors as
`{"error": {"code": "<SNAKE_CASE>", "message": "..."}}` with 400/401/403/404/409/422/429;
controllers delegate to the **same services** as the web controllers.

| Method | Path | Maps to service | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/token` | `UserService` + login throttle | email+password → `{accessToken, refreshToken, expiresIn}` |
| POST | `/api/v1/auth/refresh` | new | rotate refresh token |
| GET | `/api/v1/me` | `AccountService` | user, plan, limits (`PlanLimits`) |
| GET | `/api/v1/chats?q&filter&mailbox&page` | `ConversationListService` | page of conversation summaries + counts |
| GET | `/api/v1/chats/{key}` | `ChatService` | bubble runs, participants; marks read |
| POST | `/api/v1/chats/{key}/messages` | `ReplyService` | `{body, subject?, asReply}` |
| GET | `/api/v1/mailboxes` | `MailAccountService` | list + status |
| POST | `/api/v1/devices` | new (Web Push roadmap) | push token registration |

Exact JSON schemas are **TBD** and will be fixed here before the first endpoint is coded.
