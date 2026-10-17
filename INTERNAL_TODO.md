# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No mobile REST API or token auth exists; the Flutter client has nothing to call (`docs/API.md`). Next run: design
  bearer auth + read-only `/api/v1/conversations` slice (service-layer reuse), add Flyway migration for tokens.
- `/chats/events` SSE payload schema is undocumented.

## Logic drift
- Several `/chats` controller methods (`ConversationController`) hold orchestration (mailbox selection, recipient
  derivation, attachment checks) that a REST controller would have to duplicate; extract into a service first.

## Repo hygiene
- `Dockerfile`, `docker-compose.yml`, `.dockerignore`, `.github/workflows/ci.yml` are tracked even though the
  roadmap says they were removed on 2026-06-11 (owner decision: do not re-add). Owner to confirm deletion.
- `master/` directory holds PLAN/BACKLOG/CHANGELOG (3.5k lines) in parallel with `CLAUDE.md` roadmap; root
  `CHANGELOG.md` now points at it. Consolidate.
- Branch policy conflict: CLAUDE.md says "do not push to main"; sync routine says commit to master. This run used
  the session-designated feature branches instead.

## Missing tests / tooling
- Backend gates not run in the Phase 0 run (docs-only change).
- `flutter`/`dart` not installed in the routine environment.
