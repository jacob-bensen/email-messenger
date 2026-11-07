# API.md — canonical API contract

**Status (2026-11-07): there is no mobile REST API.** Nothing below `/api/` exists. The mobile
client (`conexus-mail-mobile`) cannot talk to the server yet. This file lists what is actually
exposed and the TBD plan; update it BEFORE adding endpoints.

## 1. Implemented JSON/machine endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header (CSRF-exempt) | Raw Stripe event JSON; 400 on invalid signature (`InvalidStripeSignatureException`) |
| GET | `/chats/events` | session | `text/event-stream`; publishes new-mail events (`NewMailEvent`) for the logged-in user |
| GET | `/actuator/health` | none | Health |

## 2. Implemented HTML surface
Form/HTML routes are listed in `WEB.md`. They are session+CSRF based and are **not** a stable contract.

## 3. Planned mobile API (TBD — not implemented, shapes not final)
Intended base `/api/v1`, bearer-token auth (session/CSRF is unsuitable for a mobile client).
Minimum viable set, mirroring existing services:
- `POST /api/v1/auth/login` → token (TBD)
- `GET /api/v1/chats?page&q&mailbox&filter` ← `ConversationListService` / `ConversationListItem`
- `GET /api/v1/chats/{key}` ← `ChatService` / `ChatConversation`
- `POST /api/v1/chats/{key}/send` ← `ReplyService`
- Errors: TBD (propose `{ "error": code, "message": text }`; plan-limit errors map from `PlanLimitExceededException`).

`ConversationListItem` (existing record, would be the list DTO): `key, title, initials, group,
memberCount, preview, lastActivity, unread, threadCount, accounts[], senderEmail`.
