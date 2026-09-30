# API contract

**Status (2026-09-30): there is NO mobile REST API yet.** This file records what exists and the
rules any new endpoint must follow. Both repos depend on it; update it *before* implementing.

## Existing machine-consumable endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — pulls new mail for all the user's active mailboxes |
| GET | `/chats/events` | session | – | `text/event-stream`; events published from `NewMailEvent` (payload shape TBD — read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe event JSON | `200`; `400` on invalid signature |
| GET | `/actuator/health` | none | – | Spring Boot health JSON |

All other routes are HTML form/redirect flows (see `WEB.md`) and are **not** part of the client contract.

## Errors
`GlobalExceptionHandler` renders HTML error pages (413 for oversized uploads: 10MB/file, 25MB/request).
No JSON error envelope exists yet — TBD when the API is designed.

## Rules for the future `/api/v1/**`
- JSON only; Bearer-token auth (design TBD — needs a new Flyway migration for token storage and a
  second `SecurityFilterChain` with CSRF disabled for `/api/**`).
- Controllers stay thin and call the same services the Thymeleaf controllers use
  (`ConversationListService`, `ChatService`, `OutboundMessageService`, `MailAccountService`).
- Conversations are keyed by `conversationKey` (participant-set key), not thread id.
- Plan limits are enforced server-side (`PlanLimitService`); the client mirrors them for UX only.
