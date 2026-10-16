# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No REST API / token auth exists for the mobile client (`docs/API.md` §Planned). Needs: `/api/v1` security chain
  (stateless Bearer, CSRF off), JSON error handler, DTOs; first candidate feature: login + conversation list.
- `GET /chats/events` SSE payload is undocumented.

## Logic drift
- Routine assumes jOOQ; the code uses JPA. Decision recorded: keep JPA, no jOOQ introduced.
- Web controllers (`ConversationController`) assemble view models inline; extract shared service methods
  (list/open/send) as DTO-returning services before adding API controllers, to avoid forked logic.

## Missing tests / verification
- `./mvnw verify` not run in the 2026-10-16 bootstrap run (docs only); see CHANGELOG.
- IMAP IDLE not verified against a real server (see CLAUDE.md roadmap).

## Other
- CLAUDE.md says "do not push to main"/branch-per-session; the sync routine prompt says commit to `master`.
  Sessions are pinned to `claude/*` branches by the harness, so work lands there.
