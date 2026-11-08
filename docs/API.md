# API contract (canonical) — status: NO MOBILE API YET

Last audited: 2026-11-08. This file lists what the server **actually exposes**. Anything marked TBD is not implemented; do not build clients against it until it is specified here and implemented.

## Current non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` (synchronous mailbox refresh for the user) |
| GET | `/chats/events` | session | none | SSE stream (`text/event-stream`) of new-mail signals (`ChatEventStream`); event payload shape TBD (not documented in code audit) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 2xx on success; `InvalidStripeSignatureException` on bad signature |
| GET | `/actuator/health` | public | — | Spring Actuator health |

All other routes return server-rendered HTML (see `WEB.md`) and use form posts with CSRF tokens + session cookies.

## Data a mobile client will need (internal view models, not yet exposed as JSON)
- `ConversationListItem`: key, title, initials, group, memberCount, preview, lastActivity (UTC), unread, threadCount, accounts[], senderEmail.
- `ChatConversation`: key, title, initials, members[], threadCount, runs[] (`BubbleRun` → `BubbleMessage`: messageId, bodyHtml, sentAt, attachments[], outbound, replyToMessageId).
- Send: body (≤100000 chars), optional subject, `asReply`, attachments (multipart), `mailbox` scope.

## TBD — proposed v1 mobile API (NOT implemented)
Prefix `/api/v1`, JSON, bearer-token auth (`POST /api/v1/auth/token`), endpoints for conversation list, conversation detail (+mark read), send, new chat, mailbox list, live events. Needs a design decision on token auth (first-party token table vs. JWT) before implementation — see INTERNAL_TODO.
