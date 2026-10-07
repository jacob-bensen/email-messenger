# API contract

**Status (2026-10-07): there is no mobile REST API.** The server exposes only
cookie-session, CSRF-protected, server-rendered web endpoints. This file is the
canonical contract; sections below separate what *exists* from what is
*proposed* (not implemented — **TBD**, do not code against it until promoted).

## 1. Existing JSON / streaming endpoints
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": int}` |
| GET | `/chats/events` | session | none | `text/event-stream`, per-user `NewMailEvent`s (event schema **TBD**, see `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | Stripe event JSON | 2xx; invalid signature → error via `InvalidStripeSignatureException` |
| GET | `/actuator/health` | public | — | Spring health JSON |

Errors: HTML error page (`error.html`) via `GlobalExceptionHandler`; no JSON
error envelope exists yet.

## 2. Existing HTML endpoints
See `WEB.md` for the full route inventory. They return HTML/redirects and are
not part of the mobile contract.

## 3. Proposed mobile API v1 (NOT IMPLEMENTED — TBD)
Prefix `/api/v1`, JSON, bearer-token auth (new; sessions+CSRF are unsuitable for
mobile). Minimal first slice, in dependency order:
1. `POST /api/v1/auth/login` `{email,password}` → `{accessToken,refreshToken,expiresIn}`; `POST /api/v1/auth/refresh`.
2. `GET /api/v1/chats?mailbox=&q=&filter=&page=` → page of conversation list items (wraps `ConversationListService.list`).
3. `GET /api/v1/chats/{key}` → conversation with bubble runs (wraps `ChatService`), marks read.
4. `POST /api/v1/chats/{key}/messages` `{body, subject?, asReply?}` → outbound message (wraps the same path as `ConversationController.send`).
5. Error envelope: `{"error":{"code":"…","message":"…"}}` with 400/401/403/404/409/429; plan limits → 402/403 + `PlanLimitKind`.
Open questions: token storage entity + Flyway migration, push notifications (roadmap item "Web Push"), attachment download (blob store not built).
