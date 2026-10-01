# Internal TODO (severity-ordered)

## Contract breakage
- No mobile REST API: `docs/API.md` is a plan only. Client cannot talk to server (session+CSRF HTML only). Next feature: stateless token auth + `GET /api/v1/chats`, `GET /api/v1/chats/{key}`.

## Logic drift
- Sync-routine spec assumes jOOQ; the backend uses JPA/Hibernate. Keep JPA (don't introduce jOOQ) — spec assumption is outdated.
- Web controllers (`ConversationController`) assemble model attributes inline (scope/mailbox selection, filters); extract into a service before an API duplicates it.

## Missing tests / unverified
- `./mvnw test` not run in the bootstrap run (docs only).
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Process
- Routine prompt says commit to `master`; this session is pinned to branch `claude/gifted-carson-foql6s` and CLAUDE.md forbids pushing to main. Resolve which wins.
- Existing CLAUDE.md roadmap items remain the product backlog.
