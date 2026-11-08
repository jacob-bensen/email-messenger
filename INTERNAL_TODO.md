# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No `/api/v1` REST API or token auth exists; mobile client has nothing to call. First feature run: define contract in `docs/API.md`, add bearer auth filter, and a read-only `GET /api/v1/conversations` + `GET /api/v1/conversations/{key}` backed by `ConversationListService`/`ChatService` (same services as the web).
- No JSON error envelope; `/chats/events` SSE payload undocumented.

## Logic drift
- Branch policy conflict: routine says commit to `master`; the cloud session pins branch `claude/gifted-carson-agwr9r`. This run committed on the session branch. Owner should decide.
- Stack mismatch with routine assumptions: repo uses JPA, not jOOQ (no codegen).
- `master/` directory at repo root holds PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS from an earlier routine; reconcile with root `CHANGELOG.md`.
- `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` still exist though CLAUDE.md says Docker was removed (2026-06-11).

## Missing tests
- Not audited this run (`./mvnw test` not run; see CHANGELOG).

## Style
- CLAUDE.md "Tech stack" section is stale relative to actual state.
