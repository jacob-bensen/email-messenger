# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No JSON API / token auth exists, so the mobile client cannot talk to the server at all. Implement `docs/API.md` §Proposed (start with auth token + conversation list + conversation detail).
- `/chats/events` SSE payload schema undocumented.

## Logic drift / structure
- Controllers (e.g. `ConversationController`) mix parsing, mailbox selection and model-building; extract so a REST controller can reuse it without duplicating (`selectMailbox`, `parseFilter`, `scopeAddress` live in the controller).
- Stack deviation: JPA instead of jOOQ — keep JPA; don't introduce jOOQ without a decision.

## Housekeeping
- `CLAUDE.md` says Docker and GitHub Actions CI were removed (2026-06-11) but `Dockerfile`, `docker-compose.yml`, `.dockerignore`, `.github/workflows` still exist. Decide: delete or update CLAUDE.md.
- `master/` directory holds legacy planning docs (PLAN, BACKLOG, 500KB CHANGELOG) that collide with the branch name `master` in git commands; consider moving to `docs/legacy/` (use `git ... --` meanwhile).
- Routine says "work only on master"; this environment pins feature branch `claude/gifted-carson-seuipq`. Runs follow the environment-pinned branch.

## Missing tests
- No tests for any JSON API (none exists).
