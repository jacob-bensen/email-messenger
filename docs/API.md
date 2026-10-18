# API.md — canonical API contract

Both repos depend on this file. Status legend: **IMPLEMENTED** (matches code as of
2026-10-18) or **PLANNED** (contract proposal; not yet in code — implement server
first, then client, in the same run that flips it to IMPLEMENTED).

## 1. Implemented machine-facing endpoints

| Method / path | Auth | Notes |
|---|---|---|
| `POST /billing/webhook` | none; `Stripe-Signature` header; CSRF-exempt | Body: raw Stripe event JSON. `200 "ok"`, `400 "missing Stripe-Signature header"` / `"invalid signature"`, `503 "webhook not configured"`, `500 "error"` (Stripe retries). Not for the mobile client. |
| `POST /chats/refresh` | session cookie + CSRF | Triggers an immediate import for the user's active mailboxes. `200 {"imported": <int>}`. |
| `GET /chats/events` | session cookie | SSE (`text/event-stream`) of new-mail events for the user. Event payload schema: TBD (see `NewMailEvent`/`ChatEventStream`). |
| `GET /actuator/health` | none | Spring Actuator health. |

Everything else is HTML (see `WEB.md`). Errors on HTML routes render `error.html`.

## 2. Mobile REST API — PLANNED (`/api/v1`), DRAFT

**Nothing below exists yet.** Design constraints: reuse the services behind the web
controllers; server is authoritative; JSON; errors as
`{"error": {"code": "<SNAKE_CASE>", "message": "<text>"}}`.

### Auth (PLANNED)
- `POST /api/v1/auth/token` — body `{email, password}` → `200 {accessToken, refreshToken, expiresIn}`; `401 INVALID_CREDENTIALS`; `429 TOO_MANY_ATTEMPTS` (reuse `LoginThrottleService`).
- `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`.
- Google sign-in for mobile: TBD (ID-token exchange).
- All other endpoints: `Authorization: Bearer <accessToken>`, no CSRF.

### Conversations (PLANNED)
- `GET /api/v1/conversations?q=&mailbox=&filter=all|unread|groups&page=0` → `{items:[ConversationListItem], counts, page, hasMore}`.
  `ConversationListItem`: `{key, title, initials, group, memberCount, preview, lastActivityEpochMilli, unread, threadCount, accounts[], senderEmail}` (mirrors the Java record).
- `GET /api/v1/conversations/{key}` → `ChatConversation`: `{key, title, initials, members:[{email,displayName,initials,label,signatureHtml}], threadCount, runs:[…]}` (run/bubble schema TBD from `SenderBubbleRun`/`BubbleMessage`). Marks read.
- `POST /api/v1/conversations/{key}/messages` → `{body, subject?, asReply?, mailboxId?}`; `201` with the recorded message; `400 EMPTY_MESSAGE`, `404`, `409 NO_RECIPIENTS`.
- `POST /api/v1/conversations/refresh` → `{imported}`.
- Live updates: `GET /api/v1/events` SSE (or push — TBD).

### Mailboxes / account / billing (PLANNED, TBD)
Mailbox list/connect/sync, plan & limits (`FREE|PRO|BUSINESS`, see `PlanLimits`),
Stripe checkout stays web-only.
