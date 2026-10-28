# API.md — canonical API contract

Status (2026-10-28): **no mobile/REST API is implemented.** This file records what exists (all session-cookie + CSRF
based, designed for the browser) and reserves `/api/v1/**` for the mobile client. Nothing under `/api/` exists in code;
do not implement the client against it until an endpoint is specified here **and** shipped.

## 1. Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous mailbox refresh for the signed-in user |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); emits "new mail" signals (`ChatEventStream`, `NewMailEvent`). Event payload shape: TBD |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 2xx on handled; invalid signature rejected (`InvalidStripeSignatureException`) |
| GET | `/actuator/health` | public | — | Spring Boot health |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | public | — | PWA assets |

Error format for these: TBD (HTML `error.html` via `GlobalExceptionHandler`; not JSON).

## 2. Form/HTML endpoints useful as the future API's behavioral reference
See `WEB.md`. Key behaviors a JSON API must reuse via the same services:
- List chats: `GET /chats?page&q&mailbox&filter` (filter ∈ `ConversationListService.ChatFilter`), page size in `ConversationController.PAGE_SIZE`.
- Open chat: `GET /chats/{key}` (marks read). Send: `POST /chats/{key}/send` multipart `body` (≤100 000 chars, may be empty if attachment), `subject?`, `asReply` (bool), `attachments[]`, `mailbox?`.
- Mailboxes: `/mailboxes` (list/new/create/{id}/sync/{id}/delete/{id}/signature/preferences), Gmail OAuth `/mailboxes/gmail/{connect,reconnect/{id},callback}`.
- Saved searches: `POST /searches`, `POST /searches/{id}/delete`. Billing: `POST /billing/checkout|portal|cancel-subscription`.

## 3. Planned mobile API (`/api/v1`) — NOT IMPLEMENTED
Prerequisites (see INTERNAL_TODO P0): bearer-token auth (login/refresh/revoke), CSRF-exempt `/api/**` chain, JSON error
envelope, then endpoints: `POST /api/v1/auth/login`, `GET /api/v1/chats`, `GET /api/v1/chats/{key}`,
`POST /api/v1/chats/{key}/messages`, `GET /api/v1/mailboxes`. Schemas to be written here first.
