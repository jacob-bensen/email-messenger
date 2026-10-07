# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style. Last updated 2026-10-07.

## Contract breakage
- **No REST API exists** and the mobile repo is empty; `docs/API.md` has no `/api/v1`. Blocks all
  client/server feature work. Next run: pick the auth scheme (token vs. cookie), then ship a minimal
  `POST /api/v1/auth/login` + `GET /api/v1/chats` (list) end-to-end with the Flutter client.
- **No non-cookie auth**: `SecurityConfig` only supports form/OIDC sessions + CSRF.

## Logic drift (risk)
- `ConversationController` (504 lines) owns mailbox selection/scoping (`selectMailbox`, `scopeAddress`,
  filter parsing) and send orchestration. Extract into a service before an API controller needs it,
  so web and API share one path.

## Doc/stack mismatches with the routine's target stack
- Routine assumes jOOQ; backend uses Spring Data JPA/Hibernate. No codegen step exists. Decision: keep JPA
  (docs/CLAUDE.md state this).
- Branching: routine says `master`; this session is bound to branch `claude/gifted-carson-npkk8b`
  (and `CLAUDE.md` forbids pushing to main). Work is pushed to the session branch; merge to `master` is manual.
- Stale routine docs in `master/` (BACKLOG/CHANGELOG/PLAN/MASTER_ACTIONS) from an older routine; root
  `CHANGELOG.md` is now the live one. Consider archiving `master/`.

## Missing tests / verification
- `mvn -B test` on 2026-10-07: 794 run, 0 failures, 0 errors, 1 skipped (Docker-only). `-B verify` not run.

## Deferred (from CLAUDE.md roadmap)
Support email, Google OAuth consent publish, Web Push, delivery states, signature contact card,
attachment storage/download, reply-to-specific-bubble.
