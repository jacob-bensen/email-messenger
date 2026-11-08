# API.md — canonical API contract

**Status (2026-11-08): there is no JSON API for the mobile client yet.** This file documents what the server
exposes today, then the planned `/api/v1` surface as **TBD** (nothing below the TBD heading is implemented).
Rule: update this file *before* changing either repo's implementation.

## 1. Existing non-HTML endpoints (implemented)
| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronous poll of the user's mailboxes |
| `GET /chats/events` | session | none | `text/event-stream`; event `new-mail` data `{"imported":<int>}`; `: ping` comment keep-alives |
| `POST /billing/webhook` | none; `Stripe-Signature` header | raw Stripe JSON | `200`; `400` missing/invalid signature; `503` webhook unconfigured |
| `GET /actuator/health` | none | — | Spring Boot health JSON |

## 2. Auth today
Cookie session from form login `POST /login` (form fields `email`, `password`, `remember-me`, `_csrf`). All other routes return HTML (302 to `/login` when unauthenticated).

## 3. Planned `/api/v1` (TBD — not implemented, not a commitment)
Needs a token auth scheme first. Candidate surface, mapped to existing services:
- `POST /api/v1/auth/login` → access/refresh tokens
- `GET /api/v1/chats?q&mailbox&filter&page` (ConversationListService)
- `GET /api/v1/chats/{key}` (ChatService; marks read)
- `POST /api/v1/chats/{key}/messages` (OutboundMessageService)
- `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/{id}/sync`
Error envelope, DTO schemas: TBD when the first endpoint is specified.
