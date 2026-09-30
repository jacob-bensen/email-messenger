# API contract (canonical, shared with `conexus-mail-mobile`)

## Status: no mobile REST API is implemented yet

Verified 2026-09-30 by scanning all controllers: every user-facing route returns HTML/redirect
and is protected by session cookie + CSRF. Nothing below `/api/**` exists. Any client
work must start by defining the contract here (contract-first) and then implementing it.

## Non-HTML endpoints that exist today
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header; CSRF exempt | Raw JSON from Stripe. Not for the mobile client. |
| GET | `/chats/events` | session | `text/event-stream` (SSE) of `NewMailEvent`s; event payload schema TBD (read `ChatEventStream`). |
| GET | `/manifest.webmanifest`, `/sw.js`, `/icons/*` | public | PWA assets. |
| GET | `/actuator/health` | public | Health. |

## HTML workspace routes (reference for mapping to a future JSON API)
`GET /chats` (params `page`, `q`, `mailbox`, `filter`), `GET /chats/new`, `POST /chats/new`
(`to`, `subject`, `body`, `attachments[]`, `mailbox`), `POST /chats/refresh`,
`GET /chats/{key}`, `GET /chats/{key}/pane`, `POST /chats/{key}/send`
(`body`, `subject`, `asReply`, `attachments[]`, `mailbox`), `/mailboxes/**`, `/searches/**`.

## Planned v1 (TBD — not implemented, not agreed)
Proposed first slice: token auth (`POST /api/v1/auth/token`), `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/messages`. Schemas, error envelope and
versioning to be defined here before any code is written.
