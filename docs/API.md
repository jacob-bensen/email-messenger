# API contract (canonical for backend + mobile)

**Status: no JSON API for the mobile client exists yet.** The server today exposes only HTML pages (see
`docs/WEB.md`) plus the machine endpoints below. The mobile repo (`conexus-mail-mobile`) must not call anything
else until a versioned `/api/v1` section is added here *first* (contract-first rule).

## Existing non-HTML endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream` (SSE); emits a signal when new mail arrives for the user. Browser-only today (cookie session). |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | Stripe webhook receiver (`BillingWebhookController`, `@RestController`). |
| GET | `/actuator/health`, `/actuator/health/**` | public | Health. |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml`, `/icons/*` | public | PWA / SEO assets. |

Error shape for these: standard Spring (`/error` page / HTTP status). No JSON error envelope defined (TBD).

## Planned /api/v1 (TBD — not implemented, not a commitment)

Needs, in order: token auth (login → access/refresh token; Google sign-in for mobile), `GET /api/v1/me`,
`GET /api/v1/chats` (rail), `GET /api/v1/chats/{key}` (bubble runs), `POST /api/v1/chats/{key}/send`,
`GET /api/v1/mailboxes`. DTOs should be records mirroring `ConversationListItem`, `ChatConversation`, `BubbleRun`,
`BubbleMessage`. Each must be specified here (schemas + error codes) before implementation.
