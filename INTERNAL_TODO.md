# Internal TODO (ordered by severity)

## Contract breakage
- No REST API / token auth exists; `docs/API.md` is a stub and the mobile repo
  has no code. Design `/api/v1` + token auth, then build the client.

## Logic drift / docs
- Roadmap in `CLAUDE.md` says Docker and CI were removed (2026-06-11), but
  `Dockerfile` and `docker-compose.yml` are still in the repo root. Decide:
  delete, or update CLAUDE.md.
- Routine assumes jOOQ; repo uses JPA. Documented in `docs/BACKEND.md`.
- Per-page service usage in `docs/WEB.md` not yet audited for logic that lives
  in controllers rather than services.
- SSE `/chats/events` payload shape undocumented.

## Missing tests
- None audited yet.

## Process
- Scheduled routine says "work on master"; the session harness pins feature
  branches (`claude/*-dogkaj`). Commits went to the pinned branches.
