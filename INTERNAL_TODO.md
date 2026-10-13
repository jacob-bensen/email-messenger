# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` JSON API or token auth exists; mobile client cannot talk to the server. First mobile feature must define it in docs/API.md, then implement via existing services (`ChatService`, `ReplyService`, `MailAccountService`).

## Logic drift
- Controllers (`ConversationController`) mix selection/validation logic with rendering; extract reusable parts into services before exposing via REST.
- Stack deviation: JPA instead of jOOQ (decide whether to keep; generic routine assumes jOOQ).

## Missing tests / gates
- `mvn verify` not yet run in this environment (see CHANGELOG 2026-10-13).
- Flutter/Dart not installed in the routine environment; frontend gates cannot run.

## Housekeeping
- A stray `master/` directory (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS) shadows the `master` ref in git commands (`git diff master` is ambiguous); use `origin/master` or move it.
- Dockerfile/docker-compose.yml exist although CLAUDE.md says Docker was removed.
