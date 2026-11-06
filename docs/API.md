# API contract (canonical for backend ⇄ mobile)

**Status (2026-11-06): no mobile REST API exists.** The backend exposes only
session-cookie, CSRF-protected, server-rendered routes (inventory in `WEB.md`).
Nothing below is invented; the mobile client has no code yet.

## Existing machine-readable endpoints
| Method/Path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — synchronously polls the user's mailboxes |
| `GET /chats/events` | session | none | SSE stream; event `new-mail`, data `{"imported":<int>}` |
| `POST /billing/webhook` | Stripe signature | Stripe event JSON | 200 / 400 on bad signature |
| `GET /actuator/health` | public | – | Spring Boot health |

## Planned `/api/v1` surface (TBD — to be designed one feature per run)
Decisions needed before any implementation:
1. **Auth**: bearer tokens (access + refresh) issued by `POST /api/v1/auth/login`
   and Google ID-token exchange; stateless filter chain separate from the web chain
   (CSRF off for `/api/**`).
2. **Resources** (all backed by existing services): conversations list/detail
   (`ConversationListService`, `ChatService`), send (`OutboundMessageService`,
   `ReplyService`), mailboxes (`MailAccountService`), account/plan
   (`PlanLimitService`), push registration (roadmap: Web Push part 2).
3. **Error model**: `{ "error": "<code>", "message": "<text>" }` with HTTP status;
   plan-limit errors map from `PlanLimitExceededException` (→ 402/403, TBD).

Recommended first slice: auth + `GET /api/v1/conversations`.
