# API contract (canonical)

Status: **no mobile REST API exists yet.** The server is a session-cookie, CSRF-protected,
server-rendered app. This file lists the only machine-facing endpoints that exist today and the
**planned** v1 mobile contract (marked PLANNED — not implemented; neither repo may rely on it until
implemented and the marker is removed).

## Existing machine-facing endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | `text/event-stream` of new-mail signals (payload shape TBD — see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature (`Stripe-Signature`), no CSRF | Stripe event JSON | 200 / 400 on invalid signature |
| GET | `/actuator/health` | public | — | Spring health JSON |

All other routes return HTML; see `docs/WEB.md`.

## PLANNED: `/api/v1` mobile contract
Design constraints: stateless bearer token auth (new), JSON only, server authoritative, same services as web.
- PLANNED `POST /api/v1/auth/login` `{email,password}` → `{accessToken, refreshToken, expiresIn}`; errors `401 invalid_credentials`, `429 throttled`.
- PLANNED `GET /api/v1/conversations?cursor=` → `{items:[ConversationListItem], nextCursor}`.
- PLANNED `GET /api/v1/conversations/{key}` → bubble runs (`BubbleRun`/`BubbleMessage` shape).
- PLANNED `POST /api/v1/conversations/{key}/messages` `{body}` → created message.
Schemas are TBD until the first is implemented (see `INTERNAL_TODO.md`).
