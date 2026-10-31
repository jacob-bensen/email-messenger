# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No REST API/bearer auth exists; mobile client cannot talk to the server. `docs/API.md` §2 is only a proposal. (Next run: implement token auth + `GET /api/v1/chats`.)

## Logic drift
- `ConversationController.send` holds send logic (recipient resolution, signature, reply-vs-new). Extract to a service before adding a REST endpoint so web and API share it.
- `/chats/events` event payload undocumented.

## Docs / repo hygiene
- Root `CHANGELOG.md` was missing; the legacy 500KB log lives in `master/CHANGELOG.md` (with PLAN/BACKLOG/MASTER_ACTIONS). Decide whether to fold `master/` into `docs/`.
- `Dockerfile`, `docker-compose.yml`, `.dockerignore`, `.github/` exist although CLAUDE.md says Docker and CI were removed (owner decision 2026-06-11). Confirm with owner; not touched.
- Routine prompt assumes jOOQ; backend uses JPA. No codegen needed.
- Routine says commit to `master`; this session was assigned feature branches (`claude/gifted-carson-mbui7c`, `claude/trusting-cori-mbui7c`) — worked there, no PRs.

## Missing tests
- None audited yet this run (no code changed).
