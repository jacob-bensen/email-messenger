# API contract (canonical)

Status as of 2026-10-27: **no JSON REST API exists.** The mobile client repo (`conexus-mail-mobile`) is an empty scaffold. This file records the endpoints that exist today that a client could touch, and is where the mobile REST contract (`/api/v1/**`) must be defined *before* implementation (see `INTERNAL_TODO.md` #1).

Auth today: browser session cookie (`JSESSIONID`) obtained via `POST /login` form (`email`, `password`, CSRF token). Errors are rendered HTML (`error.html`), not JSON.

## Non-HTML endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream` (SSE) of new-mail events for the signed-in user. Event payload schema: TBD (see `NewMailEvent`, `ChatEventStream`). |
| POST | `/billing/webhook` | public, Stripe signature | Body: raw Stripe event JSON; header `Stripe-Signature` required. 200 ok; 400 missing/invalid signature; 503 webhook not configured; 500 apply failure (Stripe retries). |
| GET | `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/robots.txt`, `/sitemap.xml` | public | PWA / SEO assets |
| GET | `/actuator/health` | public | Health |

## Planned mobile API (not implemented — TBD)

To be designed: token auth (login/refresh), conversation list, conversation messages, send/reply, mailbox list/connect status, plan/limits, push-registration. Do not implement client code against this section until it is specified here with request/response schemas and error codes.

HTML routes are inventoried in `docs/WEB.md`.
