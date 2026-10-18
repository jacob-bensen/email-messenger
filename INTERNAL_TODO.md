# Internal TODO (severity-ordered)

## Contract breakage
- No JSON API or token auth exists; the mobile client has nothing to call. Design `/api/v1/**` (see `docs/API.md` Proposed) with bearer auth in `SecurityConfig` (separate filter chain, CSRF off for it).

## Logic drift
- Controllers (`ConversationController`) build view models inline; extract a controller-independent facade (list/open/send) so the future API reuses it rather than duplicating `populateList`/`chatFor`.

## Missing tests / unverified
- Not run this session: `mvn verify` (no `~/.m2` cache; see CHANGELOG). 
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Docs/style
- `SSE` event names/payloads undocumented in `API.md`.
- Stack drift: task template assumes jOOQ; repo uses JPA.
- Branching: routine prompt says `master`; session harness mandates feature branch `claude/gifted-carson-03a4dz`. Work was committed there.
