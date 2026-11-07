# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No REST API exists for the mobile client; `docs/API.md` is a draft. First feature run should implement auth (bearer token) + conversation list/read.
- [ ] No stateless/token auth; security chain is session+CSRF only (`SecurityConfig`).

## Logic drift
- [ ] Controllers (e.g. `ConversationController`) assemble view models inline via `Model`; extract DTO-producing service methods so REST and Thymeleaf share them.

## Environment / process
- [ ] Routine prompt assumes jOOQ + `master` branch; repo uses JPA and this session was assigned branch `claude/gifted-carson-s50vqj` (platform instruction wins). Update the routine prompt or merge branches manually.
- [ ] Maven deps not cached in this sandbox; verify gate status each run.
- [ ] `master/` dir holds legacy BACKLOG/CHANGELOG/PLAN docs; decide whether to fold into `docs/`.
- [ ] Roadmap items in CLAUDE.md (support email, Google OAuth publish, Web Push, delivery states, ...) remain open.

## Missing tests
- [ ] None audited yet for API (no API).
