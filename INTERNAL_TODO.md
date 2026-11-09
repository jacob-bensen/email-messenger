# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No `/api/v1` REST layer or token auth exists; the mobile client has nothing to call. First feature run: define auth + read-only chat list/messages in docs/API.md, then implement via existing services.

## Logic drift
- Routine assumes jOOQ; repo uses JPA. Routine/docs should say JPA.
- `/chats/events` SSE event payload is undocumented.

## Missing tests / gates
- `mvn verify` not run in the Phase 0 session (docs-only change).

## Process
- Routine says commit to `master`; the session harness mandated branch `claude/gifted-carson-w2rboa`. Reconcile.
- Planning docs live in `master/` (PLAN/BACKLOG/MASTER_ACTIONS/CHANGELOG, 500KB) — root CHANGELOG.md now points there.
