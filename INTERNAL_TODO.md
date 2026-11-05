# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No mobile REST API exists; the sibling repo `conexus-mail-mobile` has no code. Design + implement `/api/v1` (auth first) and record in docs/API.md.

## Logic drift
- Services currently take `User` entities resolved from `Principal`; extract an auth-agnostic entry so a token-authenticated API can reuse `ChatService`/`ReplyService` unchanged.

## Missing tests
- None audited this run (no code changed).

## Style / process
- Routine assumes jOOQ; repo uses JPA. Decision: keep JPA.
- Routine says commit to `master`; session/CLAUDE.md require the designated feature branch (`claude/gifted-carson-2bu38o`). Branch used; owner to confirm.
- Existing operational notes live in `master/` (PLAN.md, BACKLOG.md, MASTER_ACTIONS.md, 500KB CHANGELOG.md). Consider reconciling with root CHANGELOG.md.
- Quality gates not run this run (docs-only).
