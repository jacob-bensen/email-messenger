# API.md — canonical API contract

**Status (2026-11-05): no JSON REST API is implemented.** Nothing below `/api/v1` exists yet; the
mobile client cannot talk to the server. Everything marked PLANNED is a proposal, not behavior.
When an endpoint is implemented, change its status to IMPLEMENTED in the same commit and mirror it in
`conexus-mail-mobile/docs/MOBILE.md`.

## Conventions (PLANNED)
- Base path `/api/v1`, JSON (`application/json`), UTF-8, ISO-8601 UTC timestamps.
- Auth: bearer token (see Auth). Browser session cookies are NOT accepted on `/api/**` (no CSRF exposure).
- Errors: `{"error":{"code":"<SNAKE_CASE>","message":"..."}}` with HTTP 400/401/403/404/409/422/429.
  Plan-limit failures: 403 `PLAN_LIMIT_EXCEEDED` with `kind` (from `PlanLimitKind`).
- Paging: `page` (0-based), `size` (≤50); response `{items, page, size, totalElements}`.
- Every endpoint delegates to existing services; no logic in controllers.

## Auth (PLANNED, TBD design)
| Endpoint | Purpose |
|---|---|
| `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}` | reuse `UserService`/`AuthenticationManager`; honor `LoginThrottleService` |
| `POST /api/v1/auth/refresh` `{refreshToken}` → same | rotating refresh tokens (needs migration) |
| `POST /api/v1/auth/logout` | revoke refresh token |
Google sign-in for mobile: TBD.

## Conversations (PLANNED — first feature to implement)
| Endpoint | Backed by |
|---|---|
| `GET /api/v1/conversations?mailbox=&q=&filter=ALL\|UNREAD\|AWAITING\|ATTACHMENTS&page=&size=` → page of `ConversationListItem` + `counts` (`ChatCounts`) | `ConversationListService.list/counts` |
| `GET /api/v1/conversations/{key}?mailbox=` → `ChatConversation` (key,title,initials,members[],threadCount,runs[]); marks read | `ChatService.markRead` + `chatFor` logic (currently private to `ConversationController` — extract first) |
| `POST /api/v1/conversations/{key}/messages` `{body, subject?, asReply?}` → 201 message | `OutboundMessageService` / `ReplyService` |
| `GET /api/v1/mailboxes` | `MailAccountService.list` |
Exact JSON field lists: TBD, derive from the records above when implementing.

## Existing non-HTML endpoints (IMPLEMENTED)
| Method/Path | Auth | Notes |
|---|---|---|
| `GET /chats/events` | session | SSE (`text/event-stream`); `NewMailEvent` payloads from `ChatEventStream` |
| `POST /billing/webhook` | Stripe signature | Stripe events |
