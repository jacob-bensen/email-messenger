# API.md — canonical API contract

Status 2026-10-12: **no mobile REST API exists yet.** Section 1 documents the
machine-facing endpoints that do exist; section 2 is the *planned* `/api/v1`
contract (NOT implemented — client must not call it until a feature run
implements it and moves it into section 1).

## 1. Implemented (session-cookie auth, CSRF where noted)

| Method & path | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — messages newly imported by a synchronous refresh of the user's mailboxes | `302 /login` unauthenticated |
| `GET /chats/events` | session | none | `text/event-stream` SSE; emits an event when `NewMailEvent` fires for the user (event names/payload: TBD — read `ChatEventStream`) | `302 /login` |
| `POST /billing/webhook` | Stripe signature, CSRF-exempt | raw Stripe event JSON + `Stripe-Signature` header | `200` | `400` missing/invalid signature, `503` webhook not configured, `500` processing error (Stripe retries) |
| `GET /actuator/health` | public | — | Spring Actuator health | — |

All other routes return HTML (see `docs/WEB.md`).

## 2. Planned `/api/v1` (proposal, unimplemented)

Principles: JSON only; stateless bearer-token auth (needs a new Flyway table
for refresh tokens); every endpoint scoped to the authenticated user; handlers
call the same services as the web controllers (`ChatService`,
`ConversationListService`, `MailAccountService`, `OutboundMessageService`,
`BillingService`). Errors use `{"error": "<code>", "message": "<text>"}`.

| Method & path | Purpose |
|---|---|
| `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}` | token login |
| `POST /api/v1/auth/refresh` | rotate tokens |
| `GET /api/v1/me` → `{id,email,plan}` | profile + plan (`FREE`/`PRO`/`BUSINESS`) |
| `GET /api/v1/conversations?q=&cursor=` | conversation list (rail) |
| `GET /api/v1/conversations/{key}` | bubble runs for a conversationKey |
| `POST /api/v1/conversations/{key}/messages` `{body,asReply}` | send |
| `GET /api/v1/mailboxes`, `POST /api/v1/mailboxes/{id}/sync` | mailbox state |

First implementation target recommended in `INTERNAL_TODO.md`.
