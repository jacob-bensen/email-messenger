# API contract (canonical)

**Status: no mobile REST API exists yet.** This file records what is actually exposed (verified in
code on 2026-10-11) and reserves the planned `/api/v1` surface as TBD. Do not implement client code
against TBD sections until they are specified here and implemented server-side in the same run.

## Auth
Session cookie + CSRF token (`_csrf` form field or `X-CSRF-TOKEN`). No bearer tokens. TBD: mobile token auth.

## Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); emits new-mail events published by `MailboxPollingService` / `ImapIdleService`. Event names/payload: TBD (see `ChatEventStream`, `NewMailEvent`) |
| POST | `/billing/webhook` | public; `Stripe-Signature` header | raw Stripe payload | `200`; `400` missing/invalid signature; `503` webhook not configured |

Unauthenticated access to protected routes redirects (302) to `/login`; there are no JSON error bodies.

## Planned `/api/v1` (TBD — not implemented)
Auth, conversation list, conversation detail (bubble runs), send/reply, mailbox list. To be specified
before the first mobile feature run.
