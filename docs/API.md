# API.md — canonical contract

**Status (2026-10-09): there is NO REST/JSON API for the mobile client.** The server only exposes
server-rendered HTML (see WEB.md) plus the machine endpoints below. Any mobile endpoint must be added
here *first* (contract-first), implemented on the backend, then consumed by `conexus-mail-mobile`.

## Machine-facing endpoints that exist today
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header | CSRF-exempt; Stripe events (`StripeEvent`); `InvalidStripeSignatureException` → 400 (TBD verify exact code) |
| GET | `/chats/events` | session cookie | `text/event-stream` (SSE); `NewMailEvent` signals + `ping` comments; page refreshes rail/pane on signal |
| GET | `/actuator/health` | none | health |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml` | none | PWA/SEO |

## Planned (not implemented) — mobile API v1
TBD. Prerequisites: token auth (e.g. `POST /api/v1/auth/token`), JSON DTOs for conversation list,
conversation messages, send reply, mailbox status. Error envelope proposal:
`{"error":{"code":"string","message":"string"}}`. Not binding until implemented.
