# API contract

**Status (2026-10-21): there is no mobile REST API.** The server is session-cookie + CSRF + server-rendered HTML.
This file records (A) the machine-readable endpoints that exist today and (B) the *planned* `/api/v1` surface (TBD — nothing below §B is implemented; do not build clients against it until the contract is finalized and the server ships it).

## A. Existing non-HTML endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` — synchronous poll of the user's active mailboxes |
| GET | `/chats/events` | session | none | `text/event-stream`; emits new-mail events published by `MailboxPollingService` (event names/payload: TBD, see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, CSRF exempt | Stripe event JSON | `2xx` |
| GET | `/actuator/health` | public | — | Spring health JSON |

Errors: HTML error page (`error.html`) for browsers; no JSON error envelope yet (`GlobalExceptionHandler` is view-based).

## B. Planned `/api/v1` (TBD, not implemented)
Minimum surface the mobile client needs, mirroring the web workspace and reusing its services:
- `POST /api/v1/auth/login` → bearer/refresh token (needs a token filter in `SecurityConfig`; CSRF-exempt under `/api/**`).
- `GET /api/v1/chats?mailbox=&q=&filter=all|unread|awaiting|attachments&page=` → page of `ConversationListItem` (`key,title,initials,group,memberCount,preview,lastActivity(epoch ms),unread,threadCount,accounts[],senderEmail`) + `ChatCounts{all,unread,awaiting,attachments}`.
- `GET /api/v1/chats/{key}` → `ChatConversation{key,title,initials,members[{email,displayName,initials,label,signatureHtml}],threadCount,runs[...]}`; marks read.
- `POST /api/v1/chats/{key}/send` `{body, subject?, asReply}` (+ attachments, multipart) → 201.
- `GET /api/v1/mailboxes`, `POST /api/v1/chats/refresh`, SSE or push for live updates.
