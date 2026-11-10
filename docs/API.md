# API contract

**Status (2026-11-10): there is no mobile/JSON REST API.** The backend is a session-cookie, CSRF-protected,
server-rendered Thymeleaf app (see `docs/WEB.md`). The only machine-readable endpoints are listed below.
The frontend repo (`conexus-mail-mobile`) contains only a README, so there is no client to drift from yet.

Every future endpoint MUST be added here *before* implementation (contract first), under `/api/v1/**`,
and be implemented on top of the existing service layer (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, …), never by calling controllers.

## Existing non-HTML endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/chats/events` | session | — | `text/event-stream` of new-mail events for the signed-in user (`ChatEventStream`, payload shape **TBD** — read `NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe JSON | 200 on success; `InvalidStripeSignatureException` → 4xx |
| GET | `/actuator/health` | public | — | Spring Boot health JSON |
| GET | `/manifest.webmanifest`, `/sw.js`, `/robots.txt`, `/sitemap.xml` | public | — | static-ish |

Error format for HTML routes: `error.html` via `GlobalExceptionHandler` (oversized upload → 413). No JSON
error envelope exists. **TBD.**

## Planned (not implemented) — proposal for `/api/v1`

Nothing here is implemented; it is a starting point to be finalised in the first feature run.

- Auth: `POST /api/v1/auth/token` (email+password → bearer token, refresh), `POST /api/v1/auth/google`
  (ID-token exchange). Needs a stateless `SecurityFilterChain` for `/api/**` with CSRF off.
- `GET /api/v1/conversations?q&filter&page&mailbox` → page of conversation list items
  (mirror `ConversationListItem`).
- `GET /api/v1/conversations/{key}` → `ChatConversation` (title, members, bubble runs).
- `POST /api/v1/conversations/{key}/messages` (text + attachments) → delegates to `ReplyService`.
- `POST /api/v1/conversations` (new message) → `OutboundMessageService`.
- `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/{id}/sync`.
- Push/live updates: reuse `ChatEventStream` over SSE with bearer auth, or FCM (roadmap item "Web Push").

## Sync audit
- Server endpoints vs client calls: client has no code → no mismatches, but also no mobile-capable auth.
- Web vs API duplication: no REST API exists, so no divergent logic. Web controllers go through services.
