# API.md — canonical API contract (backend ⇄ mobile client)

**Status: no mobile API exists yet.** The server exposes no JSON endpoints for a client; the
mobile app (`conexus-mail-mobile`) has no code. This file is the place the contract must be
defined *before* implementing (Phase 1, "contract first").

## Existing machine-facing endpoints (not for the mobile client)
| Method | Path | Auth | Body | Notes |
|---|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe event JSON | `BillingWebhookController`; invalid signature → `InvalidStripeSignatureException` |
| GET | `/chats/events` | session | — (SSE `text/event-stream`) | emits new-mail events for the signed-in user |

## Conventions to adopt for the mobile API (proposed, TBD)
- Base path `/api/v1`, JSON, `Authorization: Bearer <token>` (requires new token auth; session+CSRF is unsuitable for a native client).
- Server authoritative; same service layer as web controllers (`ConversationListService`, `ChatService`, `ReplyService`, `MailAccountService`).
- Error body: `{ "error": "<code>", "message": "<text>" }`; plan-limit violations map from `PlanLimitExceededException` (HTTP 402/403, TBD).
- First candidate feature set: login → list chats → open chat → send reply → live updates.
