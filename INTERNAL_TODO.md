# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] **No mobile API exists.** Frontend repo (`conexus-mail-mobile`) is an empty README; backend has no `/api/v1`, no token auth. Define the v1 contract in `docs/API.md` (draft is there), add stateless bearer auth (separate `SecurityFilterChain` for `/api/**`), then implement list/read/send over the existing services. Highest-value next feature (needs both repos).
- [ ] `/chats/events` SSE payload shape is undocumented — read `ChatEventStream` and document.

## Logic drift
- [ ] Controllers inject repositories/services directly; before adding the API, confirm `ConversationController` holds no business logic that the API would need to duplicate (extract into `ChatService`).

## Deviations from the routine's assumed stack
- Persistence is JPA, not jOOQ — no codegen step. Keep JPA.
- A `master/` directory at repo root holds older routine notes (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS) — unrelated to the `master` git branch; consider consolidating into `CHANGELOG.md`.
- Branching: the routine says commit to `master`; this session's harness mandates `claude/gifted-carson-5hgtwp` (backend) and `claude/trusting-cori-5hgtwp` (frontend), and the repo CLAUDE.md forbids pushing to main. Work was pushed to those branches.

## Missing tests / gates
- [ ] Run `./mvnw test` baseline in a Docker-capable environment (not run in the 2026-10-27 docs-only run).
