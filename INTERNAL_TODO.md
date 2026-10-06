# INTERNAL_TODO (ordered by severity)

## Contract breakage
- [ ] **No mobile REST API or token auth exists**; `docs/API.md` PLANNED v1 is unimplemented and the mobile repo is empty. First feature: bearer-token auth + `GET /api/v1/chats` (+ detail) delegating to `ConversationListService`/`ChatService`. Needs a new `SecurityFilterChain` for `/api/**` (stateless, CSRF off for it only).
## Logic drift
- [ ] None found yet; re-audit once `/api/**` exists (must share services with `ConversationController`).
## Missing tests
- [ ] No API/controller tests for `/api/**` (nothing to test yet).
## Style / docs
- [ ] Stack deviates from routine's assumed jOOQ (uses JPA); docs state this.
- [ ] Legacy `master/CHANGELOG.md` (500KB) predates root `CHANGELOG.md`; consider archiving.
- [ ] `/chats/refresh` response schema undocumented (TBD).
- [ ] Routine says commit to `master`; this session is pinned to feature branches and CLAUDE.md forbids pushing main — branch used instead.
