# API.md — canonical contract

**Status: no mobile REST API exists yet.** Everything below describes what the
server actually exposes today. The mobile client MUST NOT assume any `/api/**`
route until it is added here first (contract-first rule). Items marked TBD are
unimplemented.

## Conventions today
- Auth: session cookie (`JSESSIONID`) obtained via `POST /login`
  (`application/x-www-form-urlencoded`: `email`, `password`, optional `remember-me`),
  plus CSRF token on every non-GET (form field `_csrf`) except the webhook.
- Responses are HTML (Thymeleaf) or redirects, except where noted.
- Error responses: standard Spring `/error` HTML page. No JSON error schema (TBD).

## Machine-consumable endpoints (implemented)

### `GET /chats/events` — auth required
Server-Sent Events (`text/event-stream`), per user.
- Event `new-mail`, data `{"imported": <int>}` — emitted after a poll/IDLE import.
- Comment `: ping` keep-alives.

### `POST /billing/webhook` — public, signature-verified
Stripe webhook. Header `Stripe-Signature` required. Body raw Stripe JSON.
Errors: 400 `missing Stripe-Signature header` / `invalid signature`; 503
`webhook not configured`. CSRF-exempt. Not for the mobile client.

### Public non-HTML
`GET /manifest.webmanifest`, `/sw.js`, `/icons/*`, `/robots.txt`, `/sitemap.xml`,
`/actuator/health` (public).

## Mobile API (TBD — to be designed)
Proposed first slice (NOT implemented): token auth (`POST /api/v1/auth/login`),
`GET /api/v1/chats`, `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`,
backed by `ConversationListService`, `ChatService`, `ReplyService`. Needs a
decision on token scheme (see INTERNAL_TODO S1).

## HTML route inventory
See `docs/WEB.md`.
