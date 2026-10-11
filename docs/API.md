# API.md — canonical API contract (client ⇄ server)

**Status (2026-10-11): there is NO versioned JSON API.** The mobile repo
(`conexus-mail-mobile`) is an empty scaffold, so nothing is out of sync yet — but
the contract must be agreed before either side codes against it.

## A. What exists today (JSON/stream-capable surface)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | — | `text/event-stream` new-mail events (payload shape TBD — see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe event JSON | Stripe-only; not for clients |
| GET | `/actuator/health` | none | — | Spring health JSON |

Everything else returns HTML or redirects (see `WEB.md`). Unauthenticated
requests get a 302 to `/login`; CSRF failures 403; `GlobalExceptionHandler`
renders HTML error pages.

## B. Planned `/api/v1` (NOT IMPLEMENTED — design target, details TBD)
Principles: reuse existing services; stateless token auth on a separate
`securityMatcher("/api/**")` chain (CSRF off there); JSON errors
`{"error":{"code","message"}}`; timestamps as epoch millis (matches existing
`*EpochMilli` accessors).

First slice (read-mostly chat client):
1. `POST /api/v1/auth/login` `{email,password}` → `{accessToken, refreshToken, expiresAt}` (Google sign-in on mobile: TBD)
2. `GET /api/v1/chats?page&q&mailbox&filter` → page of `ConversationListItem`
   (`key,title,initials,group,memberCount,preview,lastActivity(ms),unread,threadCount,accounts[],senderEmail`)
3. `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[],threadCount,runs[]`; bubbles:
   `messageId,bodyHtml,sentAt(ms),attachments[],outbound,replyToMessageId`); marks read
4. `POST /api/v1/chats/{key}/send` `{body, subject?, asReply?}` (attachments TBD)
5. `GET /api/v1/chats/events` SSE or push (TBD)

Any change here must land with server + client implementations together.
