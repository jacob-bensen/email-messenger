# API.md — canonical API contract

**Status: no mobile/REST API exists yet.** Contract version: `0` (nothing implemented).
This file records (A) the machine-readable endpoints that exist today and (B) the planned `/api/v1`
surface for the mobile client, marked TBD. Section B is a proposal, NOT implemented on either side;
neither repo may depend on it until it is promoted to section A in the same run as its implementation.

## A. Existing non-HTML endpoints (cookie session + CSRF unless noted)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session, CSRF | none | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | none | `text/event-stream`; events published by `ChatEventStream` on `NewMailEvent` (payload shape: TBD, see `ChatEventStream.java`) |
| POST | `/billing/webhook` | Stripe signature header, no CSRF | raw Stripe event JSON | 200 / 400 on bad signature |
| GET | `/manifest.webmanifest` | public | — | PWA manifest JSON |
| GET | `/actuator/health` | public | — | Spring Boot health |

Errors for these routes are produced by `GlobalExceptionHandler` / Spring defaults; the JSON error
envelope is TBD.

## B. Planned `/api/v1` (TBD — not implemented)
Prerequisite: token auth (e.g. `POST /api/v1/auth/login` → bearer access+refresh token; stateless
filter chain scoped to `/api/**`, CSRF off for it). Candidate first slice:
- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- `GET /api/v1/chats?page=&q=&mailbox=&filter=` → conversation list
- `GET /api/v1/chats/{key}` → bubble runs
- `POST /api/v1/chats/{key}/send`
Schemas, error codes and pagination to be defined when implemented. Must reuse `ConversationService`,
`ChatService`, `ReplyService` (same as the Thymeleaf controllers).
