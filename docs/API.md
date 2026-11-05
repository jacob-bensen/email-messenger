# API — canonical contract between backend and mobile client

**Status (2026-11-05): no mobile/JSON API is implemented.** The backend serves only
server-rendered HTML (see `WEB.md`). Therefore this contract is intentionally empty of endpoints;
nothing here is invented. Every endpoint added later MUST be documented here first.

## Conventions to adopt (decisions pending — all TBD until the first endpoint ships)
- Base path `/api/v1`, JSON, UTF-8. TBD: error envelope `{code,message}`.
- TBD: auth for mobile. Current auth is session cookie + CSRF only (`SecurityConfig`); a native
  client needs either a token scheme (recommended: short-lived access + refresh token stored
  server-side) or cookie+CSRF handling. Decide before the first endpoint.
- Identity of a conversation: `conversationKey` (string, path param `{key}`).

## Existing machine-readable endpoints (not mobile-contract, listed for completeness)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream` (new-mail events) |
| POST | `/billing/webhook` | Stripe signature | Stripe event ingest |

## Candidate v1 surface (PLAN, not implemented)
Sign-in/refresh, list mailboxes, list conversations (filter/search/page), get conversation
(bubble runs), send message, mark read, SSE/push for new mail. Mirrors `ConversationController`,
`MailAccountService`, `ChatService`, `ReplyService`.
