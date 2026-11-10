# API contract

**Status: no mobile REST API exists yet.** This file is the canonical contract. Section 1 documents
what is actually exposed today (all session-cookie + CSRF, designed for the browser). Section 2 is the
*proposed* `/api/v1` surface — marked **PLANNED**; nothing in it is implemented. Update this file
before implementing any of it, then update the Flutter client (`conexus-mail-mobile`) in the same run.

## 1. Existing machine-readable endpoints (implemented)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` — synchronously polls the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; event `new-mail` data `{"imported": int}`; `: ping` comments as keepalive |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | `200`; invalid signature → error (see `BillingWebhookController`) |
| GET | `/actuator/health` | public | — | Spring health JSON |

Error format for these: default Spring (`/error`); no structured error schema (TBD).

## 2. PLANNED `/api/v1` (not implemented)
Auth: `POST /api/v1/auth/login` {email,password} → {accessToken, refreshToken, expiresIn} (requires adding
token auth to `SecurityConfig`; Google sign-in via ID-token exchange TBD).
Read: `GET /api/v1/chats?page&q&mailbox&filter` → paged conversation list (mirrors `ConversationListService`);
`GET /api/v1/chats/{key}` → bubble runs (mirrors `ChatConversation`); `GET /api/v1/mailboxes`.
Write: `POST /api/v1/chats/{key}/send` {body, subject?, asReply} (via `OutboundMessageService`);
`POST /api/v1/chats/{key}/read`.
Live: reuse SSE `/chats/events` with Bearer auth.
Errors: `{ "error": code, "message": string }` with 400/401/403/404/409(plan limit)/429.
DTO field-level schemas to be written when the first endpoint is chosen.

## 3. Web routes
Browser form routes are listed in `WEB.md`; they are not part of the mobile contract.
