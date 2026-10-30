# API.md — canonical API contract

Status 2026-10-30: **there is no mobile REST API yet.** This file documents (A) what exists today and (B) a *proposed* `/api/v1` surface. Section B is a plan, not implemented; the mobile client must not call it until it moves to section A.

## A. Implemented non-HTML endpoints
Auth for all: session cookie; state-changing requests need a CSRF token (header `X-CSRF-TOKEN`) except the webhook.

### POST /chats/refresh
Polls the user's mailboxes now. Auth: session. Response `200 application/json`: `{"imported": <int>}`.

### GET /chats/events
Server-Sent Events, auth: session. Event `new-mail`, data `{"imported": <int>}`. Comment `ping` keepalive every 25 s (`mailbox.sse.keepalive-ms`). Emitter timeout 30 min; client should reconnect.

### POST /billing/webhook
Stripe webhook, public, CSRF-exempt, verified by `Stripe-Signature`. Invalid signature → 4xx (`InvalidStripeSignatureException`). Not for the mobile client.

### Everything else
HTML form endpoints; see `WEB.md`. Error pages are rendered by `GlobalExceptionHandler` (HTML; plan-limit and upload-too-large have friendly pages, 413 for oversize uploads).

## B. Proposed `/api/v1` (NOT implemented — TBD)
Prerequisite: token auth (e.g. `POST /api/v1/auth/token` with email+password → bearer token; Google sign-in for mobile TBD) and a JSON error envelope `{"error": {"code": "...", "message": "..."}}`. Candidate first slice, all served by the same services as the web pages (`ConversationListService`, `ChatService`, `OutboundMessageService`):
- `GET /api/v1/chats?q&mailbox&filter&page` → page of conversation list items
- `GET /api/v1/chats/{key}` → `ChatConversation` (marks read)
- `POST /api/v1/chats/{key}/messages` → send
Request/response schemas: TBD when the slice is chosen.
