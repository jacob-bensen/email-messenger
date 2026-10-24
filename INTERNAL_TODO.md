# INTERNAL_TODO (backend)

Ordered by severity. Updated 2026-10-24.

## Contract breakage
- No mobile-consumable API exists. All endpoints are session+CSRF HTML routes;
  the Flutter client (conexus-mail-mobile, currently empty) cannot call anything
  except `/chats/refresh`, `/chats/events` (session cookie only). Needs
  `/api/v1` with token auth (see docs/API.md §2).
- No defined JSON error shape.

## Logic drift
- Not applicable yet (no second consumer). When `/api/v1` is added it must call
  `ChatService` / `ConversationListService` / `OutboundMessageService`, not
  controller code (parts of list/filter parsing live in `ConversationController`
  e.g. `parseFilter`, `selectMailbox` — extract to a service first).

## Missing tests / verification
- `./mvnw test` not yet run in this audit (see CHANGELOG entry for result).

## Deviations from the generic routine
- Persistence is JPA, not jOOQ; no codegen.
- Existing project history lives in `master/CHANGELOG.md` (500KB) with
  `master/PLAN.md`, `BACKLOG.md`; new entries are appended there. Roadmap is in
  CLAUDE.md.
- The routine says commit to `master`; the session harness requires the
  `claude/gifted-carson-lnd8ww` branch, which was used.
