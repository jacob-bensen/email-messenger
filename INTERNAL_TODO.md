# Internal TODO (severity-ordered)

## Contract breakage
- No REST API / token auth for the mobile client; mobile repo is empty. First feature run: define `/api/v1` auth +
  conversation list in docs/API.md, then implement both sides.
## Logic drift
- Run's target stack assumes jOOQ; repo uses JPA. Treat JPA as authoritative; do not introduce jOOQ.
- Task prompt says commit to `master`; session is pinned to feature branches (`claude/gifted-carson-rlzmbp`,
  `claude/trusting-cori-rlzmbp`). Branch policy needs reconciling by the owner.
## Missing tests / verification
- Quality gates not yet run in this environment (see CHANGELOG).
## Style
- README says "in development / scaffolded"; stale vs CLAUDE.md status.
