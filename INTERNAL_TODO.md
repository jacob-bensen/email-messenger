# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No `/api/v1` JSON API and no token auth: the mobile repo has nothing to call.
- No JSON error envelope; errors are HTML.

## Logic drift
- `ConversationController` holds send/scope orchestration (mailbox selection, signature,
  reply-vs-new, recipients). Extract to a service before exposing via API so web and API share it.
- Task prompt assumes jOOQ; code uses JPA. Docs follow the code; decide whether to keep JPA.

## Missing tests / verification
- Verify IMAP IDLE against a real IDLE-capable server (from CLAUDE.md roadmap).
- `/chats/events` SSE payload format undocumented precisely (see `ChatEventStream`).

## Style / housekeeping
- `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` exist though CLAUDE.md says Docker was removed 2026-06-11.
- CLAUDE.md "Tech stack (target)"/"Commands" sections are stale (project is scaffolded).
- Repo has `master` plus a session branch; the sync routine says commit to master, but this
  session was constrained to branch `claude/gifted-carson-456o0l`, so docs were pushed there.
