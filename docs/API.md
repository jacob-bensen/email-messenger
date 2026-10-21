# API CONTRACT

Canonical contract between backend and `conexus-mail-mobile`. Status: **no mobile API exists yet.**

## Implemented non-HTML endpoints (all session-cookie + CSRF unless noted)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session | none (CSRF header) | `200 {"imported": <int>}` — synchronous mailbox sync |
| GET | `/chats/events` | session | — | `text/event-stream`; emits new-mail signals (`NewMailEvent(userId, importedCount)`) |
| POST | `/billing/webhook` | Stripe signature, no CSRF | raw Stripe JSON | 200 / 400 on bad signature |
| GET | `/actuator/health` | none | — | Spring health |
| GET | `/manifest.webmanifest`, `/sw.js` | none | — | PWA assets |

Errors: `GlobalExceptionHandler` maps `IllegalArgumentException` → 400 and renders `error.html`
(HTML, not JSON).

## Everything else is HTML
Routes are listed in `WEB.md`. Clients must not scrape them.

## Planned: `/api/v1` (PROPOSED — not implemented, shapes TBD until the feature run)
Prerequisites (see `INTERNAL_TODO.md`): token auth (login → bearer token), JSON error envelope,
CSRF exemption for `/api/**`, public service-layer access from an `api/` package.
Candidate first slice, mapped to existing view records:
- `POST /api/v1/auth/login` → token
- `GET /api/v1/chats?page&q&mailbox&filter` → page of `ConversationListItem`
  (`key,title,initials,group,memberCount,preview,lastActivity,unread,threadCount,accounts,senderEmail`)
- `GET /api/v1/chats/{key}` → `ChatConversation` (`key,title,initials,members[ChatMember],threadCount,runs[…BubbleMessage]`)
- `POST /api/v1/chats/{key}/messages` (reply), `POST /api/v1/chats` (new)
