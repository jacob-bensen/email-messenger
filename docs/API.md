# API contract

**Status: no mobile-oriented REST API exists yet.** The server is a session-authenticated,
CSRF-protected, server-rendered app. This file documents only what is actually implemented;
the planned mobile API is TBD and must be specified here *before* implementation.

## Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — polls the user's mailboxes now |
| GET | `/chats/events` | session | none | `text/event-stream`; event `new-mail`, data `{"imported": <int>}` |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | handled by `BillingWebhookController` (errors: invalid signature → 4xx) |
| GET | `/actuator/health` | none | | Spring health |

Error format: TBD (HTML error page via `GlobalExceptionHandler` / `error.html`).

## Planned mobile API (TBD — not implemented)
Needs decisions first: token auth (e.g. bearer/JWT or API keys), JSON resources for
conversations (`conversationKey`), messages/bubbles, send/reply, mailboxes, plan limits,
and push registration. Do not implement client code against these until specified here.
