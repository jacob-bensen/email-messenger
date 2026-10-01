# API.md — canonical API contract

**Status: no mobile REST API is implemented yet.** This file (1) records the non-HTML endpoints that exist
today, and (2) fixes the *planned* `/api/v1` contract the Flutter client will be built against. Anything under
"PLANNED" is a proposal, not behavior: update this file first, then implement both sides.

## A. Existing non-HTML endpoints (implemented)
| Method + path | Auth | Request | Response |
|---|---|---|---|
| `POST /chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — polls user's mailboxes now |
| `GET /chats/events` | session | none | `text/event-stream`; emits a signal when new mail is imported (`ChatEventStream`, `NewMailEvent`). Event names/payload: TBD (read `ChatEventStream` before relying on them) |
| `POST /billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe event JSON | `200` ok; invalid signature → error (`InvalidStripeSignatureException`) |
| `GET /actuator/health` | none | — | Spring Boot health |
| `GET /manifest.webmanifest` | none | — | PWA manifest |

Errors for web routes render `error.html` via `GlobalExceptionHandler` (e.g. `IllegalArgumentException` → 400).
Error body shape for JSON: TBD.

## B. PLANNED `/api/v1` (not implemented)
Blocking decision (see INTERNAL_TODO #1): mobile auth. Proposal: `POST /api/v1/auth/login`
`{email,password}` → `{accessToken, refreshToken, expiresIn}`; bearer token on all other calls; Google sign-in
via ID-token exchange. Until decided, treat the shapes below as draft.

Conversation shapes mirror existing records `ConversationListItem` and `ChatConversation`:

- `GET /api/v1/conversations?q&filter=all|unread|...&mailbox&page` → `{items:[{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}], page, pageSize:30}`
- `GET /api/v1/conversations/{key}` → `{key,title,initials,members:[{email,...}],threadCount,runs:[...]}` and marks read (same as `ChatService.markRead` + `buildFor`)
- `POST /api/v1/conversations/{key}/messages` `{body, subject?, asReply?}` → sent message (same path as `ConversationController.send`: `ReplyService` + `OutboundMessageService`)
- `POST /api/v1/sync` → `{imported}`
- `GET /api/v1/mailboxes`, plan/limits endpoints: TBD

Error envelope (proposed): `{ "error": { "code": "string", "message": "string" } }` with 400/401/403/404/409/429.
Plan-limit violations map from `PlanLimitExceededException` → 403 code `plan_limit`.
