# API CONTRACT (canonical)

**Status 2026-10-23: no mobile REST API exists.** The mobile client
(`conexus-mail-mobile`) has no code yet. This file documents the HTTP surface that
*actually exists today* (all session-cookie + CSRF, browser oriented) and reserves the
`/api/v1` namespace for the first mobile feature, whose contract MUST be written here
before implementation.

## Conventions (current)
- Auth: session cookie; unauthenticated → 302 to `/login`. CSRF token required on every
  POST except `/billing/webhook`.
- Errors: HTML error page via `GlobalExceptionHandler`/`error.html`; no JSON error envelope.

## JSON / stream endpoints that exist
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | none | `200 {"imported": <int>}` — synchronous mailbox refresh for the active user |
| GET | `/chats/events` | session | none | `text/event-stream` (SSE); emits new-mail events published by `MailboxPollingService` (event payload shape: TBD, see `NewMailEvent`, `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header | raw Stripe JSON | 2xx / signature error (`InvalidStripeSignatureException`) |

## HTML-only endpoints (inventory in `WEB.md`)
Everything else (chats, mailboxes, account, billing, team, admin) returns HTML or
redirects and is **not** a client contract.

## Planned `/api/v1` (TBD — not implemented)
Needs a decision on token auth (bearer/JWT or API session), JSON error envelope, and
DTOs mirroring `ConversationListItem` / `ChatConversation`. Tracked in `INTERNAL_TODO.md`.
