# Internal TODO (severity-ordered)

## Contract breakage
- No mobile REST API or token auth exists; `docs/API.md` "Planned" section is the starting point. Blocks all client features.
- `/chats/events` SSE payload schema undocumented.

## Logic drift
- Routine assumes jOOQ; backend uses JPA. Treat JPA as canonical (no codegen step).
- Web controllers (`ConversationController`) hold some orchestration; extract to services before adding an API layer so both share it.
- `ReplyService` always threads onto the last message (see CLAUDE.md roadmap).

## Missing tests
- Not audited this run; `./mvnw test` not run (see CHANGELOG).

## Deferred
- Existing CLAUDE.md roadmap items (support email, Google OAuth publish, Web Push, delivery states, etc.).
- Routine says commit to `master`; this session is pinned to feature branch `claude/gifted-carson-i9bg1o` — resolve which applies.
