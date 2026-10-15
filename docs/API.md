# API.md — canonical contract (backend ↔ mobile)

## Status: **no mobile JSON API exists yet.**
The server currently exposes only the session-cookie, CSRF-protected, server-rendered website (`WEB.md`).
The `conexus-mail-mobile` client has no code. Nothing below §"Planned" is implemented; it is a TBD placeholder,
not a contract. Per run rules, the contract must be written here **before** implementation of any endpoint.

## Existing non-HTML endpoints (implemented)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — sync active user's mailboxes, count of new messages |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE) "new mail" signals; payload shape TBD (see `ChatEventStream`/`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe JSON | handled by `BillingWebhookController`; error codes TBD |

Errors: unauthenticated → `302 /login`; CSRF failure → `403`; oversize upload → friendly `413` via `GlobalExceptionHandler`.

## Planned (TBD — not implemented, shapes provisional)
Minimum mobile surface, all JSON under `/api/v1`, bearer-token auth (new; session/CSRF unsuitable for mobile):
- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- `GET /api/v1/chats` → list of `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail`)
- `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[]`; run→`BubbleMessage`)
- `POST /api/v1/chats/{key}/send`, `POST /api/v1/chats`
- `GET /api/v1/mailboxes`
The record names above are the existing server view models (`com.emailmessenger.service`) that these would serialize.
