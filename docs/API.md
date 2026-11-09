# API contract

**Status: no mobile REST API exists yet.** This file is the canonical contract;
it currently documents only the non-HTML endpoints that exist today. Planned
endpoints are marked PLANNED and must be agreed here before implementation.

## Existing non-HTML endpoints (session-cookie auth, CSRF token required on POST)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session | `200 {"imported": int}` |
| GET | `/chats/events` | session | `text/event-stream` (new-mail events from `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header | 200; invalid signature → error via `InvalidStripeSignatureException` |

Errors: unauthenticated requests get a 302 to `/login` (HTML), not 401 JSON.

## PLANNED (TBD — design in the next feature run)
Bearer-token auth (`POST /api/v1/auth/token`), `GET /api/v1/conversations`,
`GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`.
Prerequisite: extract controller logic into services (see INTERNAL_TODO.md).
