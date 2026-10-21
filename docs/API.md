# API contract

The canonical contract between `email-messenger` (server) and `conexus-mail-mobile`.

## Status (2026-10-21)

**No JSON API is implemented.** The server only exposes session-cookie, CSRF-protected,
HTML/form endpoints (inventory in `WEB.md`). The mobile client therefore cannot talk
to the server yet. Section 1 records what actually exists; Section 2 is the
*planned* `/api/v1` surface and is **not implemented** — do not build against it
until each endpoint is moved to "Implemented" in the same commit as its code.

## 1. Implemented, non-HTML endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | Server-Sent Events; `NewMailEvent` published by `MailboxPollingService`. Payload shape: TBD (read `ChatEventStream`). |
| POST | `/billing/webhook` | Stripe signature | Stripe events; CSRF-exempt. Not for clients. |
| GET | `/manifest.webmanifest`, `/sw.js`, `/offline`, `/icons/*` | public | PWA shell. |
| GET | `/robots.txt`, `/sitemap.xml` | public | SEO. |
| GET | `/actuator/health` | public | Liveness. |

Error handling: `GlobalExceptionHandler` renders HTML `error.html`; oversized
uploads return 413 (multipart caps 10MB/file, 25MB/request).

## 2. Planned `/api/v1` (NOT IMPLEMENTED)

Conventions: JSON, UTF-8, `Authorization: Bearer <token>`, errors as
`{"error": {"code": "...", "message": "..."}}` with 400/401/403/404/409/413/429.
Plan-limit failures reuse `PlanLimitKind` as the `code` (HTTP 403).

Proposed first slice (each reuses existing services):

- `POST /api/v1/auth/login` `{email,password}` → `{token, expiresAt}` — TBD design (token storage, refresh).
- `GET /api/v1/chats?mailbox=&q=&filter=&page=` → page of `ConversationListItem` (`ConversationListService.list`).
- `GET /api/v1/chats/{key}` → `ChatConversation` (members, bubble runs) (`ChatService`).
- `POST /api/v1/chats/{key}/send` `{body, subject?, asReply?}` → created outbound message (`OutboundMessageService`).
- `GET /api/v1/mailboxes` → `MailboxView[]`.

Exact JSON schemas: TBD — derive from the view-model records in `service/` and `web/`.
