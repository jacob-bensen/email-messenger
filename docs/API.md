# API — canonical contract

**Status (2026-10-01): there is NO mobile REST API.** The server exposes only server-rendered
HTML pages (`docs/WEB.md`) plus the machine-oriented endpoints below. The mobile repo
(`conexus-mail-mobile`) contains only a README, so there is no client to diverge yet.
Nothing under "Planned" is implemented; do not code against it until it is moved up and implemented
on both sides in the same change.

## Existing machine-oriented endpoints (as implemented)
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF token | none | `200 application/json` `{"imported": <int>}` — synchronous mailbox sync for the user |
| `GET /chats/events` | session | none | `text/event-stream` (SSE); events published from `NewMailEvent` via `ChatEventStream`. Event names/payload: TBD (read `ChatEventStream`) |
| `POST /billing/webhook` | none; Stripe signature header | raw Stripe JSON | 2xx on handled; `InvalidStripeSignatureException` on bad signature (status TBD). CSRF-exempt |
| `GET /actuator/health` | none | — | Spring Boot health JSON |

Errors: `GlobalExceptionHandler` renders HTML error pages; oversize uploads → 413 page. No JSON error envelope.

## Auth for mobile — TBD (needs a design decision)
Current auth is cookie session + CSRF (`SecurityConfig`). A mobile client needs a token scheme
(e.g. bearer access/refresh tokens) with CSRF exempted for the API prefix. Not designed.

## Planned (NOT implemented) — first proposed slice
Read-only chats, mirroring existing service records:
- `POST /api/v1/auth/login` → token pair
- `GET /api/v1/chats?mailbox=&filter=&q=&page=` → page of `ConversationListItem`
  (`key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail`)
- `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members,threadCount,runs`)
- `POST /api/v1/chats/{key}/send`, `POST /api/v1/chats/refresh`
Prerequisite: extract list/scope helpers out of `ConversationController` into services
(`INTERNAL_TODO.md` #2).
