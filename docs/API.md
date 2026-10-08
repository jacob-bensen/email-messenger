# API.md — canonical API contract

**Status (2026-10-08): no JSON REST API is implemented.** The sections below separate
what exists from what is *proposed*. Do not implement the client against PROPOSED
endpoints until they are marked IMPLEMENTED here (contract first, then both repos).

## 1. Implemented machine-readable endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session cookie | `text/event-stream`; emits new-mail events (`NewMailEvent`) per user. Exact event names/payload: TBD (read `ChatEventStream`). |
| POST | `/billing/webhook` | Stripe signature header | Raw Stripe JSON, CSRF-exempt. Not for clients. |
| GET | `/actuator/health` | public | Spring Boot health. |

All other routes return HTML (see `docs/WEB.md`), use form posts, require a CSRF token,
and redirect unauthenticated callers to `/login`. Errors are rendered via `error.html`
(`GlobalExceptionHandler`: 404 NoSuchElement, mail/import failures, billing, plan-limit, 413 upload).

## 2. PROPOSED v1 (not implemented)

Base path `/api/v1`, JSON, `Authorization: Bearer <access token>`; errors as
`{"error": {"code": "...", "message": "..."}}` with 401/403/404/409/422/429.

| Method | Path | Maps to existing service |
|---|---|---|
| POST | `/api/v1/auth/token` (`email`,`password` → access+refresh) | `UserService`/`DbUserDetailsService` |
| POST | `/api/v1/auth/refresh` | new |
| GET | `/api/v1/conversations?mailbox&q&filter&page` | `ConversationListService.list/counts` |
| GET | `/api/v1/conversations/{key}` | `ChatService.buildFor`, `markRead` |
| POST | `/api/v1/conversations/{key}/messages` | `ReplyService` + `OutboundMessageService` (needs extraction from `ConversationController`) |
| POST | `/api/v1/conversations` (new chat) | same |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` |
| POST | `/api/v1/devices` (push token) | new; depends on roadmap "Web Push" |

Open decisions (TBD): token format (opaque vs JWT), refresh rotation, whether Google
sign-in is supported on mobile (needs native OIDC flow), pagination envelope, attachment
download (blocked: `attachments.blob_ref` is always null).
