# API contract

Canonical contract between backend and `conexus-mail-mobile`.

## Status: no mobile REST API exists yet
Audit (2026-10-19): the only `@RestController` is `POST /billing/webhook` (Stripe, server-to-server). All other routes return Thymeleaf HTML or redirects and are session+CSRF protected. The client repo has no code. **Nothing below §1 is implemented; it is a proposal (TBD) and must be finalized here before any implementation.**

## 1. Implemented JSON/stream endpoints (session cookie auth)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 {"imported": <int>}` — triggers immediate mailbox poll for the user |
| GET | `/chats/events` | session | `text/event-stream` SSE; new-mail events from `ChatEventStream` (payload shape TBD — read `ChatEventStream`/`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature header; no CSRF | Stripe event ingestion |
| GET | `/actuator/health` | public | Spring actuator health |

Errors: no JSON error model. `GlobalExceptionHandler` renders an HTML `error` view with status 404 (missing), 502 (mail/billing failures); validation errors re-render forms.

## 2. Proposed v1 (NOT implemented — TBD)
Base `/api/v1`, JSON, bearer-token auth (new: login endpoint issuing a token; CSRF not applicable to bearer routes). Error envelope `{ "error": { "code": "string", "message": "string" } }`.

| Method | Path | Purpose | Backing service (to extract) |
|---|---|---|---|
| POST | `/api/v1/auth/login` | email+password → token | `UserService`/Spring auth |
| GET | `/api/v1/chats?q&filter&mailbox&page` | conversation list + counts | `ConversationListService` |
| GET | `/api/v1/chats/{key}` | conversation (members, bubble runs) | `ChatService` (`ChatConversation`) |
| POST | `/api/v1/chats/{key}/read` | mark read | `ChatService.markRead` |
| POST | `/api/v1/chats/{key}/messages` | send `{body, subject?, asReply}` | new `ChatSendService` (extract from `ConversationController.send`) |

DTOs should be records mirroring `ChatConversation`, `ChatMember`, `BubbleMessage`.
