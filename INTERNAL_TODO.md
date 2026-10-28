# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No JSON REST API exists, so the mobile client cannot talk to the backend. Need `/api/v1` + token auth
  (Spring Security: stateless filter chain scoped to `/api/**`, CSRF off there). Define in `docs/API.md` first.

## Logic drift / structure
- `ConversationController.createMessage/send` hold send orchestration (recipient parse, sender choice,
  signature, send+record). Extract into a service (e.g. `ChatSendService`) before adding REST.
- Stack mismatch vs routine assumptions: JPA not jOOQ. Docs note this; do not add jOOQ.
- `master/` directory at repo root holds stale routine files (PLAN/BACKLOG/CHANGELOG 500KB, MASTER_ACTIONS);
  consolidate or delete (needs owner decision). Root `CHANGELOG.md` starts fresh.
- Root `Dockerfile`/`docker-compose.yml`/`DEPLOY.md` exist though CLAUDE.md says Docker was removed.

## Process
- Routine says commit to `master`; session instructions pin feature branches
  (`claude/gifted-carson-pqe6yu`, `claude/trusting-cori-pqe6yu`) and CLAUDE.md forbids pushing main. Branches used.

## Missing tests
- None audited this run (docs-only). `mvn test` not run; only `test-compile` verified.
