# Internal TODO (backend)

Ordered by severity.

## Contract breakage
- [ ] No mobile REST API or token auth exists; `conexus-mail-mobile` has nothing to call. First slice
      proposal: `POST /api/v1/auth/token`, `GET /api/v1/chats`, `GET /api/v1/chats/{key}`,
      `POST /api/v1/chats/{key}/messages`; define in `docs/API.md` first.
- [ ] `/chats/events` SSE payload is undocumented in `docs/API.md`.

## Logic drift
- [ ] Audit found no web/REST duplication (no REST layer). Re-audit when `/api` lands.
- [ ] `CLAUDE.md` says Docker/CI were removed, but `Dockerfile` and `docker-compose.yml` are still in the repo root.

## Missing tests
- [ ] Not verified in this run: `./mvnw verify` (Phase 0 was docs-only).

## Style / docs
- [ ] Routine template assumes jOOQ; project uses JPA. Keep template-neutral.
- [ ] Routine says commit to `master`; this session's harness designates branch `claude/gifted-carson-d1cu02`. Work went there.
