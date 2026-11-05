# API.md — canonical API contract (client ↔ server)

**Status: no first-party JSON REST API exists yet.** The only JSON-ish
endpoints are `POST /chats/refresh` (`{"imported": int}`, session auth + CSRF)
and the SSE stream `GET /chats/events`; both are website-internal. Stripe's
`POST /billing/webhook` is server-to-server.

The mobile client (`conexus-mail-mobile`) therefore has nothing to call yet.
Everything below is the **planned v1 contract (TBD — not implemented)**; do not
code against it until a feature run marks a section "implemented".

## Conventions (planned)

- Base path `/api/v1`, JSON, UTF-8. Errors: `{"error": {"code": "...", "message": "..."}}`
  with HTTP 400/401/403/404/409/422/429.
- Auth: bearer access token issued by `POST /api/v1/auth/login`
  (`{email,password}` → `{accessToken, refreshToken, expiresIn}`); CSRF not
  required for bearer calls. Server must add a stateless filter chain for
  `/api/**` alongside the existing session chain.
- Server is authoritative; the client mirrors rules (plan limits, conversation
  keys) in `lib/domain/` but never trusts local state over server responses.

## Candidate endpoints (TBD)

| Method | Path | Maps to existing service |
|---|---|---|
| POST | `/api/v1/auth/login` | `UserService` / `DbUserDetailsService` |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `ConversationListService.list` |
| GET | `/api/v1/chats/{key}` | `ChatService` / `ChatConversation` |
| POST | `/api/v1/chats/{key}/send` | `OutboundMessageService` / `ReplyService` |
| POST | `/api/v1/chats/{key}/read` | `ChatService.markRead` |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` |
