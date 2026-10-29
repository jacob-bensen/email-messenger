# Internal TODO (severity ordered)

## Contract breakage
- No mobile REST API or token auth exists; the mobile repo has no client code
  yet. First slice proposed in `docs/API.md` §2. (Blocks all client work.)

## Logic drift
- Controllers (`ConversationController`) inject many services directly; when
  `/api/v1` is added, extract a shared facade so web and API don't fork logic.
- Stack drift: task template assumes jOOQ; repo uses JPA. Treated as JPA.

## Missing tests
- None audited this run (tests not executed; see CHANGELOG).

## Process
- Scheduled routine says "master only"; the session harness pins feature
  branches (`claude/gifted-carson-ewm07i`, `claude/trusting-cori-ewm07i`) and
  repo CLAUDE.md says never push to main. Worked on the pinned branches.
  Owner should reconcile.
