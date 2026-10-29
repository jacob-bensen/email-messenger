# API.md — canonical API contract

**Status: no mobile REST API exists yet.** The sibling client repo (`conexus-mail-mobile`) is empty
(README only), so there is nothing to diff against. This file documents what is machine-callable
today and sets the rules for the future `/api/v1` surface. Do not invent endpoints here until they
are implemented.

## Existing non-HTML endpoints (session-cookie auth + CSRF unless noted)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session | none | `200 {"imported": <int>}` — messages newly imported for the user's active mailboxes |
| GET | `/chats/events` | session | — | `text/event-stream`; `NewMailEvent`s from `ChatEventStream` (payload shape TBD) |
| POST | `/billing/webhook` | Stripe-Signature header, no CSRF | raw Stripe JSON | `200`; `400` missing/invalid signature; `503` webhook not configured |

## Planned (TBD, not implemented)
- Token auth for mobile (needs design: JWT or opaque bearer; CSRF-exempt `/api/**`).
- Read: conversation list, conversation messages (IM bubble runs), mailboxes.
- Write: send reply (`ReplyService`), mark read (`ChatService.markRead`).
- All must call the same services as the Thymeleaf controllers.
- Error envelope: TBD.
