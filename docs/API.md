# API.md — canonical API contract

**Status: no REST API for the mobile client exists yet.** This file is the contract both repos depend on;
it currently records reality and the agreed plan. Nothing below under "Planned" is implemented — the mobile
client MUST NOT call it until it moves to "Implemented".

## Implemented (machine-facing)
| Method | Path | Auth | Body | Response |
|---|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header | Stripe event JSON | 200 on handled; 400 on invalid signature (`InvalidStripeSignatureException`) — exact codes TBD |

Everything else is HTML (see `docs/WEB.md`) using session cookies + CSRF, and is **not** a stable API.
`GET /chats/events` is a session-authenticated SSE stream (`NewMailEvent`); payload schema TBD.

## Planned: `/api/v1` (TBD — design before first build)
Principles: JSON only; server authoritative; thin controllers over existing services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`); plan limits enforced
server-side via `PlanLimitService`; errors as `{ "error": "<code>", "message": "..." }` with 400/401/403/404/409/429.

Open decisions:
1. **Auth**: bearer token issued by `POST /api/v1/auth/login` (+ Google ID-token exchange), CSRF-exempt, stateless chain.
2. **First vertical slice** (recommended): login → list chats → open chat → send message → mark read.
3. Pagination shape (web uses `page` with fixed page size).
