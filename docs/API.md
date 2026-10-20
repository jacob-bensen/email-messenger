# API contract (canonical)

**Status: no mobile REST API exists yet.** This file documents (1) the machine-readable endpoints that exist today and (2) the planned `/api/v1` surface as TBD. Do not implement client calls against anything marked TBD until it is specified here and built server-side in the same run.

Auth today: session cookie from `POST /login` (form fields `email`, `password`, optional `remember-me`), CSRF token required on all POSTs except the webhook. Unauthenticated requests get a 302 to `/login`.

## Existing machine-readable endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; event `new-mail` (payload: see `ChatEventStream`, TBD exact JSON), `: ping` comments |
| POST | `/billing/webhook` | Stripe signature header | raw Stripe JSON | 200 / 400 on bad signature |

Errors: `GlobalExceptionHandler` renders HTML error pages (IllegalArgumentException → 400). No JSON error envelope yet.

## Planned `/api/v1` (TBD — not implemented)
Token auth (login/refresh), `GET /conversations`, `GET /conversations/{key}`, `POST /conversations/{key}/messages`, `GET /mailboxes`, `POST /mailboxes/{id}/sync`, push-token registration. Each must call the same services as the web controllers.
