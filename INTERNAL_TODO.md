# INTERNAL_TODO

## Sync audit (2026-10-23), by severity
1. **Contract gap**: backend exposes no REST API; mobile has nothing to call. Define auth (bearer token) + `/api/v1` per `docs/API.md` "Planned v1".
2. **Logic drift risk**: web logic lives in package-private services under `web/` (`ChatService`, `ConversationListService`); API must reuse them (widen visibility or move to `service/`).
3. **Routine/stack mismatch**: routine assumes jOOQ; repo uses JPA. Routine also says branch `master`; this session was pinned to `claude/gifted-carson-lq9jmm` (repo CLAUDE.md also forbids pushing main). Existing `master/` dir (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS) holds prior routine state; root `CHANGELOG.md` was added alongside.
4. **Stale repo state**: `Dockerfile`/`docker-compose.yml` exist although CLAUDE.md says Docker was removed (2026-06-11).
5. Gates not run this run: `mvn` available but no `~/.m2` cache/offline network not verified; Flutter/Dart not installed.
