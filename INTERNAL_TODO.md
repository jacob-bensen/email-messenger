# INTERNAL_TODO

Ordered by severity.

## Contract breakage
- No mobile REST API or token auth exists; `docs/API.md` lists only 3 non-HTML endpoints. Client repo is empty. (Next feature run: define `/api/v1` auth + read conversations contract first.)

## Logic drift
- None found (single consumer). Re-audit once an API exists: controllers in `web/ConversationController` hold some orchestration (mailbox selection, refresh) that should move into services before being shared.

## Doc / repo hygiene
- `master/` directory at repo root holds PLAN.md, BACKLOG.md, MASTER_ACTIONS.md and a 500KB CHANGELOG.md from an earlier routine; likely a path mistake. Decide whether to move to `docs/`. Not touched this run.
- Routine assumes jOOQ + `master` branch; repo uses JPA and this session's mandated branch is `claude/gifted-carson-u2x3vp`.

## Missing tests
- TBD for future API.
