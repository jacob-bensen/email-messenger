# Internal TODO (ordered by severity)

## Contract breakage
- No REST API / token auth exists, so the mobile client cannot talk to the server at all.
  Next feature run: define `/api/v1` auth + read-only conversation list in `docs/API.md`,
  then implement both sides.
- `/chats/events` SSE event name and payload are undocumented (read `ChatEventStream`).

## Logic drift
- None found yet (no second consumer of the services exists).

## Missing tests / verification
- Quality gates not run in the 2026-10-18 docs-only pass (see CHANGELOG).
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Housekeeping
- `master/` directory (tracked) duplicates the role of root docs; consider merging
  `master/CHANGELOG.md` (~500 KB) history or leaving as archive.
- Routine prompt assumes jOOQ; the project uses JPA — update the routine prompt.
