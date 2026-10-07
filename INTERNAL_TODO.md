# INTERNAL_TODO

Ordered by severity.

## Contract breakage
- No mobile-consumable API: all endpoints are session+CSRF HTML. Need `/api/v1` with token auth (see docs/API.md §2) before the Flutter client can do anything. Suggested first feature: token auth + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`, backed by the same services as the web controllers.
- `/chats/events` SSE payload shape undocumented (TBD in API.md).

## Logic drift
- Chat list/pane logic lives in `ConversationController` helpers (`selectMailbox`, `scopeAddress`, `chatFor`, `populateList`); extract into a service before exposing via REST so web and API cannot diverge.
- Routine assumes jOOQ; backend uses JPA. Update the routine prompt or ignore codegen.

## Missing tests / docs
- Flutter/Dart not installed in the cloud env; frontend gates could not run.

## Style
- Routine prompt says `master`; sessions use `claude/*` branches.
