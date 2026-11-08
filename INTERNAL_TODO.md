# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No mobile REST API or token auth exists (`docs/API.md`). The mobile repo has no
  Flutter project either. First feature candidate: `/api/v1/auth/*` (stateless chain,
  bearer tokens) + read-only `GET /api/v1/chats`.

## Logic drift
- `ConversationController.populateList` builds list/dashboard model in the controller;
  extract to a service (e.g. `ChatListService`) so web and API share it.
- Routine docs assume jOOQ; this repo uses JPA. Treat JPA as authoritative.

## Missing tests
- Not audited this run (tests not executed; see CHANGELOG).

## Style / housekeeping
- Two changelogs: `master/CHANGELOG.md` (500 KB, historic) and root `CHANGELOG.md` (new, from this routine).
- Branch conflict: routine says commit to `master`; session mandates `claude/gifted-carson-b5uj98`. Session branch used.
- Open roadmap items live in `CLAUDE.md` (support email, Web Push, delivery states, …).
