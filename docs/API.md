# API contract

**Status (2026-11-01): there is NO mobile REST API yet.** This file is the
canonical contract; every endpoint below marked *implemented* exists in code
today, and every section marked *PLANNED* is TBD and MUST be specified here
before either repo implements it. Do not invent client calls beyond this file.

## Conventions (PLANNED)
- Base path `/api/v1`, JSON, UTF-8. Errors: `{ "error": "<code>", "message": "..." }`
  with 400 validation, 401 unauthenticated, 403 forbidden / plan limit, 404, 409, 429.
- Auth: TBD. Today only cookie sessions + CSRF exist (see BACKEND.md). Proposed:
  `POST /api/v1/auth/login` → bearer token, with `/api/**` exempt from CSRF.

## Implemented endpoints (non-HTML)
| Method/Path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| `GET /chats/events` | session | none | SSE stream of new-mail events (payload shape TBD — see `NewMailEvent`, `ChatEventStream`) |
| `POST /billing/webhook` | Stripe signature header, no CSRF | raw Stripe JSON | 200; 400 missing/invalid `Stripe-Signature`; 503 not configured |
| `GET /actuator/health` | public | — | Spring health |

All other routes are HTML (see `WEB.md`).

## PLANNED mobile surface (TBD, not implemented)
Auth login/logout; list conversations (`/chats` equivalent, filters
all/unread, mailbox scope, search, paging); open conversation (bubble runs);
mark read; send/reply; list/add/sync/delete mailboxes; account/plan summary.
Each needs request/response schemas written here first.
