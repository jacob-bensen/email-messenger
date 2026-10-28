# API contract (canonical)

> **Status: no mobile REST API exists yet.** This file documents (1) what is
> actually implemented and callable programmatically today, and (2) the
> *proposed* `/api/v1` surface (marked **PROPOSED**; nothing in it is implemented
> on either side). Update this file first when implementing any endpoint.

## Implemented, non-HTML endpoints
Auth for all: session cookie + CSRF token (`X-CSRF-TOKEN`/`_csrf`), except where noted.

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session | – | `200 {"imported": int}` |
| GET | `/chats/events` | session | – | `text/event-stream`; event `new-mail`, data `{"imported": int}`; 30-min emitter timeout, client reconnects |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe JSON | 2xx / error on bad signature |

Errors: unauthenticated requests get a `302` to `/login` (not 401 JSON) — unsuitable for a mobile client.

## PROPOSED `/api/v1` (TBD — not implemented)
Prerequisite: token auth (e.g. `POST /api/v1/auth/login` → bearer token; CSRF-exempt `/api/**` chain returning `401` JSON) — design decision pending.
Candidate first slice (read-only), each a thin controller over existing services:
- `GET /api/v1/chats?q&mailbox&filter=all|unread|awaiting|attachments&page` → page of `ConversationListItem` (`ConversationListService.list`)
- `GET /api/v1/chats/{key}` → `ChatConversation` (messages, bubble runs) (`ChatService`)
- `POST /api/v1/chats/{key}/send` → `OutboundMessageService`
- `GET /api/v1/mailboxes` → `MailAccountService.list`
Error envelope (proposed): `{"error": "<code>", "message": "<text>"}`.
