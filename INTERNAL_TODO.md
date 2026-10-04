# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No REST API exists, so the mobile client has nothing to call. Next feature run:
  token auth + `GET /api/v1/chats` (read-only chat list) reusing `ConversationListService`.
- `POST /chats/refresh` JSON response is undocumented.

## Logic drift
- Not yet audited: `ConversationController` (≈330 lines) assembles view models inline;
  extract to services before exposing via REST so web and API cannot diverge.
- Task template assumes jOOQ; backend uses JPA. Treat JPA as the convention.

## Process
- Routine says "work on master"; this environment pins `claude/gifted-carson-f3jccz`
  / `claude/trusting-cori-f3jccz`. Work was committed on those branches.
- Backend CLAUDE.md says do not push to `main`; roadmap there is separate.
- Flutter/Dart not installed in the sandbox: frontend gates cannot run.
- `DEPLOY.md`/`Dockerfile`/`docker-compose.yml` still exist though the roadmap says Docker was removed.
