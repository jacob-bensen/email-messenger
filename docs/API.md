# API contract (canonical)

Status: **no JSON/REST API for the mobile client exists yet.** This file documents what the server really exposes. When a mobile endpoint is added, define it here FIRST, under "Mobile API (planned)".

## Machine-facing endpoints (implemented)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header (`Stripe-Signature`), CSRF-exempt | `BillingWebhookController`; invalid signature → exception `InvalidStripeSignatureException`. |
| GET | `/chats/events` | session cookie | `text/event-stream` (SSE). Emits a signal when `NewMailEvent` imports mail for the user; no JSON schema defined (payload TBD — read `ChatEventStream`). |
| GET | `/actuator/health`, `/actuator/health/**` | public | Spring Actuator. |

## Browser-only endpoints
HTML form/page routes are inventoried in `WEB.md`; they are not part of the mobile contract.

## Mobile API (planned) — all TBD
Needed before the client can do anything: token auth (login/refresh), `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/messages`, device registration for push. Not designed; do not implement client-side until specified here.

## Errors
Pages use `GlobalExceptionHandler` + `error.html`. A JSON error envelope is TBD.
