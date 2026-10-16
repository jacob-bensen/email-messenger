# API contract (canonical)

**Status: no mobile/JSON API exists yet.** Nothing here is invented; this file records
what is actually machine-consumable today and reserves the shape of the future API.

## Existing non-HTML endpoints
| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — new messages imported by a synchronous poll of the user's active mailboxes | 302→/login if unauthenticated; 403 on missing CSRF |
| GET | `/chats/events` | session | none | `text/event-stream` of new-mail signals (`NewMailEvent`); event payload schema TBD | 302→/login |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe JSON | 200 | `InvalidStripeSignatureException` (400, TBD exact) |
| GET | `/actuator/health` | public | — | Spring Boot health | — |

## Planned v1 mobile API (NOT implemented — design TBD)
Prerequisites, in order (tracked in INTERNAL_TODO.md):
1. Token auth for non-browser clients (`POST /api/v1/auth/token`), CSRF-exempt `/api/**`.
2. Extract send/list logic from `ConversationController` into services.
3. Endpoints (proposed, unratified): `GET /api/v1/chats`, `GET /api/v1/chats/{key}`,
   `POST /api/v1/chats/{key}/send`, `POST /api/v1/chats`, `GET /api/v1/mailboxes`.
Error envelope proposal: `{"error": {"code": "...", "message": "..."}}`.
