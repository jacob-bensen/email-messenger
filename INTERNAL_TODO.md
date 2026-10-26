# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API or token auth exists; the Flutter client has nothing to call. Design in `docs/API.md` first, then build under `/api/v1/**` reusing `ChatService`/`ReplyService`/`MailAccountService`.
- `/chats/events` SSE payload is undocumented.

## Logic drift
- `ConversationController` is the only chat entry point; when JSON endpoints land, do not duplicate its logic — extract shared methods into `ChatService` if needed.

## Missing tests / verification
- Backend build/tests were not run in the bootstrap run (see CHANGELOG).
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Style / docs
- `CLAUDE.md` says Docker was removed (2026-06-11) but `Dockerfile` and `docker-compose.yml` are still in the repo root — reconcile.
- Routine template assumes jOOQ; this repo uses JPA. Stay with JPA.
- Branch policy: sessions develop on the designated `claude/...` branch, not `master` (conflicts with the routine text; branch rule from the session wins).
