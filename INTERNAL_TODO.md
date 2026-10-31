# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No REST API exists for the mobile client; client and server cannot sync. Need: stateless auth (token) decision, then conversation list / messages / send endpoints, specified in `docs/API.md` first.
- `GET /chats/events` SSE event schema undocumented.

## Logic drift
- Conversation list/messaging logic is in web-layer services (`ChatService`, `ConversationListService`) and controllers build `Model`s directly; a JSON API should reuse these services — extract view-model → DTO mapping, don't fork logic.
- `ReplyService` always threads onto the last message (see CLAUDE.md roadmap).

## Missing tests
- No contract tests for `/chats/refresh` response shape.

## Stack deviations from the sync-routine template
- Persistence is JPA, not jOOQ (no codegen). Flutter/Dart toolchain not installed in this environment, mobile gates cannot run.

## Process
- Routine says commit to `master`; the session is pinned to feature branches (`claude/gifted-carson-osiy1v`, `claude/trusting-cori-osiy1v`) and CLAUDE.md forbids pushing to `main`. Branch instructions followed; reconcile.
