# API.md — canonical client/server contract

**Status (2026-11-10): no mobile REST API is implemented.** The server only exposes server-rendered HTML routes
(see `WEB.md`) plus two machine endpoints below. The mobile client must not be coded against anything else until
endpoints are specified here first (contract-first).

## Existing non-HTML endpoints
| method | path | auth | notes |
|---|---|---|---|
| GET | `/chats/events` | session | SSE stream (`text/event-stream`) of new-mail events (`NewMailEvent`); payload schema TBD |
| POST | `/billing/webhook` | Stripe signature | Stripe webhooks; CSRF-exempt |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml` | public | PWA/SEO |

## Planned (proposal, NOT implemented) — v1 mobile API
Conventions: base `/api/v1`, JSON, `Authorization: Bearer <token>`, errors `{ "error": "<code>", "message": "..." }`
with 400/401/403/404/409/429. Candidate first slice: `POST /api/v1/auth/token`, `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/messages`. Schemas TBD until chosen as a feature.
