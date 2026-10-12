# API contract (canonical, shared with `conexus-mail-mobile`)

## Status: NO MOBILE REST API EXISTS YET
Audit (2026-10-12): the server exposes only HTML (Thymeleaf), one ad-hoc JSON route, one SSE
stream, and the Stripe webhook. There are no `@RestController`s, no token auth, no `/api/**`
routes. The mobile client therefore has nothing to call. Everything below marked **EXISTS**
is real; everything marked **PROPOSED** is a draft to be finalized in the feature run that
implements it, *before* any code is written (contract-first).

## Conventions (PROPOSED)
- Base path `/api/v1`, JSON, UTF-8. Timestamps: UTC epoch millis (`...EpochMilli`), matching
  the existing view records.
- Errors: `{ "error": "<code>", "message": "<human text>" }` with 400/401/403/404/409/429.
- Auth: **TBD** — session cookie works today but needs CSRF; a bearer-token scheme
  (`POST /api/v1/auth/token`) is the recommended first feature. CSRF must be exempted only for
  token-authenticated `/api/**`.

## EXISTS
| Method/Path | Auth | Notes |
|---|---|---|
| `POST /chats/refresh` | session + CSRF | Response `{ "imported": <int> }`. Imports new mail for the user's active mailboxes now. |
| `GET /chats/events` | session | SSE stream (`text/event-stream`) of new-mail events; payload schema TBD (see `NewMailEvent`). |
| `POST /billing/webhook` | Stripe signature | Server-to-server only; not for the client. |

## PROPOSED (first mobile slice, read-only chats)
| Method/Path | Maps to service | Response |
|---|---|---|
| `POST /api/v1/auth/token` | `UserService`/auth | `{ token, expiresAt }` |
| `GET /api/v1/chats?q=&mailbox=&filter=&page=` | `ConversationListService.list` | `{ items: ConversationListItem[], counts, page }` |
| `GET /api/v1/chats/{key}` | `ChatService`/`ConversationService` | `ChatConversation` (runs → messages) |
| `POST /api/v1/chats/{key}/messages` | `OutboundMessageService` | `{ messageId }` |

DTO shapes are the existing records `ConversationListItem`, `ChatConversation`,
`SenderBubbleRun`, `BubbleMessage` (see `service/`); finalize field-by-field when implementing.
