# API contract (canonical)

Status (2026-10-13): **there is no REST/JSON API for the mobile client yet.** The sync audit found zero
`@RestController`s; the frontend repo is an empty Flutter placeholder. This file lists what exists and
reserves the contract the first mobile feature must define *before* implementing.

## Existing non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE stream; events from `NewMailEvent` (live inbox refresh) |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe events |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml`, `/icons/*` | public | PWA/SEO |
| GET | `/actuator/health` | public | health |

## Existing form/HTML endpoints (session + CSRF)
Public: `/`, `/pricing`, `/demo`, `/login`, `/register`, `/auth/google/start`, `/password/forgot|reset`, `/verify-email`, `/privacy`, `/terms`, `/refund`, `/digest/opt-out`.
Authenticated: `/chats`, `/chats/new` (GET/POST), `/chats/refresh` (POST), `/chats/{key}`, `/chats/{key}/pane`, `/chats/{key}/send` (POST), `/mailboxes/**` (+ `/mailboxes/gmail/**`), `/searches/**`, `/account/**`, `/billing/{checkout,portal,success,cancel-subscription}`, `/team/invite[/accept]`, `/admin/revenue` (admin).

## Planned (TBD, not implemented)
`/api/v1/**` with bearer-token auth (login → access/refresh tokens), JSON for: auth, mailboxes, conversation list, conversation detail (bubble runs), send/reply. Error envelope proposal: `{"error":{"code":"...","message":"..."}}`. Must be specified here first.
