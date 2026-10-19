# Internal TODO (ordered by severity)

## Contract breakage
- No mobile-facing JSON API exists; `docs/API.md` is empty of contract. Define `/api/v1` (auth scheme first), then implement over existing services.

## Logic drift
- None found yet: web controllers are the only consumers of the services.

## Process / premise mismatches (found 2026-10-19)
- Sync routine assumes jOOQ; this repo uses Spring Data JPA. No jOOQ codegen to run.
- Routine says commit to `master`; this session is bound to branch `claude/gifted-carson-1ayyjh`. Confirm desired policy.
- `CLAUDE.md` says the project has no CI and Docker was removed, yet `Dockerfile`/`docker-compose.yml` exist. Reconcile.
- Stray `master/` directory in the repo root (PLAN/BACKLOG/CHANGELOG). Decide whether to move into `docs/`.

## Missing tests
- Not audited this run (no build executed).

## Style
- none recorded
