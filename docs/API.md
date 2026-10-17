# API contract

**Canonical contract between the backend and `conexus-mail-mobile`.** Update this file BEFORE changing either side.

## Status (2026-10-17)
**No mobile REST API exists yet.** Only the endpoints in "Existing machine-facing endpoints" are JSON/SSE, and all
are session-cookie + CSRF protected, i.e. browser-oriented. The versioned API below is the planned shape and is
**TBD** — nothing in it is implemented.

## Existing machine-facing endpoints
| Method/Path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — imports new mail for the user's active mailboxes now |
| `GET /chats/events` | session | none | `text/event-stream` (SSE); `NewMailEvent`s published by `MailboxPollingService` / `ImapIdleService` (payload schema TBD — see `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature, CSRF-exempt | Stripe event JSON | 200 / 4xx on bad signature |

## Planned `/api/v1` (TBD — design before building)
Prefix `/api/v1`, JSON, errors as `{"error": "<code>", "message": "<text>"}`. Requires a bearer-token auth scheme
(TBD: decision needed — opaque tokens stored server-side vs. JWT) and CSRF exemption for `/api/**`.
Candidate first slice (read-only): `POST /api/v1/auth/token`, `GET /api/v1/conversations`,
`GET /api/v1/conversations/{key}`. Each handler must delegate to `ConversationListService` / `ChatService`
(the same services the `/chats` pages use).
