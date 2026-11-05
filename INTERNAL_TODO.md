# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; `docs/API.md` is entirely PLANNED. Mobile client cannot function until auth + conversations endpoints land.
- No token-based auth for non-browser clients (session+CSRF only).

## Logic drift / structure
- `ConversationController` holds private helpers (`chatFor`, `selectMailbox`, `scopeAddress`, `populateChat`) that a JSON controller would need — extract into a shared service (likely `ChatService`) before adding the API, so web and API don't fork.
- Repo has a stray `master/` directory (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS from an earlier routine) alongside CLAUDE.md roadmap; decide which is canonical. Root `CHANGELOG.md` (this routine) was created fresh.
- Generic routine assumes jOOQ; repo uses JPA. Routine prompt should be updated.

## Missing tests
- None audited yet; run `./mvnw test` baseline (not run in bootstrap session — see CHANGELOG).

## Process
- Routine says commit to `master`; the cloud session mandates feature branches (`claude/gifted-carson-opxukn`) and CLAUDE.md says never push to main. This run followed the branch rule. User must reconcile.
