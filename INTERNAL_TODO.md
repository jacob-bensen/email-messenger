# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No REST API for the mobile client. Mobile repo has no code, so nothing is *currently* broken,
  but there is no stateless auth and no JSON view of chats. First feature: `/api/v1` auth +
  `GET /api/v1/chats` + `GET /api/v1/chats/{key}`, backed by the existing services.
- `GET /chats/events` payload shape is undocumented in API.md.

## Logic drift
- Controllers (`ConversationController`) assemble model attributes inline (`populateList`);
  extract a service/DTO so web and API share the same assembly.
- Template says generic routine assumes jOOQ; project uses JPA — routine prompt out of date.

## Doc/process
- Root `CHANGELOG.md` is new; historical changelog and plan live in `master/` (CHANGELOG, PLAN, BACKLOG).
- Routine prompt says commit to `master`; this environment mandates feature branches
  (`claude/gifted-carson-l748gz`, `claude/trusting-cori-l748gz`) and no PRs — branch rule followed.

## Missing tests
- No tests for a JSON contract (none exists yet).

## Quality gates not run this run
- Docs-only changes; `./mvnw test` not run.
