# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No `/api/v1` REST API and no bearer-token auth exist; the mobile client has nothing to call. Design token auth (stateless `SecurityFilterChain` for `/api/**`, CSRF-exempt), then implement the first slice listed in `docs/API.md`.

## Logic drift / structure
- Prompt assumes jOOQ; backend uses JPA/Hibernate. Keep JPA (do not migrate); client docs must not assume jOOQ types.
- `master/` directory at repo root (BACKLOG.md, 500KB CHANGELOG.md, PLAN.md, MASTER_ACTIONS.md) is a previous routine's history. Root `CHANGELOG.md` is the new one; consider archiving `master/`.
- Controllers (`ConversationController`) build models directly from several services; extract a DTO-returning facade so REST and Thymeleaf share it.
- `CLAUDE.md` roadmap mentions Web Push, delivery states, attachment download — all would need API surface.

## Missing tests / verification
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
- Phase 0 run did not execute the test suite (docs-only); see CHANGELOG.

## Git workflow note
- Routine prompt says commit to `master`; the cloud session mandated feature branches (`claude/gifted-carson-j8g8ho`, `claude/trusting-cori-j8g8ho`). Work was pushed to those; merge to master is manual.
