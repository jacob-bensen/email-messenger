# INTERNAL_TODO (ordered by severity)

## Contract breakage
- [ ] **No REST API exists** for the mobile client; `docs/API.md` has no real endpoints. Needs
  a token auth scheme (JWT or opaque) + `/api/**` security chain (no CSRF, 401 JSON).
- [ ] Mobile repo `conexus-mail-mobile` is an empty README; Flutter project not scaffolded.

## Logic drift
- [ ] Before adding the API, extract any logic living in `ConversationController`
  (`/chats` filtering, `/chats/new`, `/send`) into services so web and API share it.
- [ ] Routine assumes jOOQ; repo uses JPA. Treat JPA as authoritative.

## Missing tests / env
- [ ] Verify `./mvnw verify` baseline in CI-less local run (not run in this session).
- [ ] Flutter/Dart not installed in the routine environment; frontend gates cannot run.

## Process
- [ ] Routine says commit to `master`; the cloud session is pinned to a feature branch
  (`claude/gifted-carson-892ipi`). Decide which governs. `master/` dir holds operator
  files (PLAN/BACKLOG/CHANGELOG) distinct from root `CHANGELOG.md`.
