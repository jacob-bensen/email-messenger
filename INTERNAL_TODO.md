# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No mobile REST API or token auth exists; `docs/API.md` §2 is only a proposal. Blocks all mobile work.
- Controllers return HTML/redirects; unauthenticated JSON clients would get a 302 to `/login`.

## Logic drift
- Controllers (`ConversationController.send`) contain orchestration (sender selection, signature, thread choice,
  attachment conversion). Extract into a service before exposing the same behavior over REST so web and API share it.
- Task routine assumes jOOQ; repo uses JPA. Decide whether to keep JPA (recommended) and treat the assumption as wrong.

## Missing tests
- TBD: audit controller slice tests for `/chats/refresh` JSON shape and SSE payload.

## Housekeeping
- Stray tracked directory `master/` (BACKLOG.md, CHANGELOG.md, PLAN.md, MASTER_ACTIONS.md) — looks like a planning folder
  with a confusing name; confirm with owner before moving.
- Routine says work on `master`; the cloud session designates `claude/gifted-carson-w5ids9`. Work was done on the designated branch.
